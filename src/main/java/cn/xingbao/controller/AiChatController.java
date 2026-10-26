package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse;
import cn.xingbao.domain.KnowledgeDocument;
import cn.xingbao.repo.KnowledgeDocumentRepository;
import cn.xingbao.service.AiChatService;
import cn.xingbao.service.EmbeddingService;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 对话控制器
 * 提供聊天接口和知识库管理接口
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {
  private final AiChatService aiChatService;
  private final KnowledgeDocumentRepository documents;
  private final EmbeddingService embeddingService;

  public AiChatController(AiChatService aiChatService, KnowledgeDocumentRepository documents, EmbeddingService embeddingService) {
    this.aiChatService = aiChatService;
    this.documents = documents;
    this.embeddingService = embeddingService;
  }

  /**
   * AI 对话接口
   * 接收用户消息和可选的 sessionId，返回 AI 回复和 sessionId
   * 首次请求时 sessionId 为空，后端自动生成并返回
   * 后续请求携带 sessionId 以维持多轮对话上下文
   *
   * @param request 包含用户消息(message)和可选的会话标识(sessionId)
   * @return AI 回复内容和 sessionId
   */
  @PostMapping("/chat")
  public ApiResponse<Map<String, Object>> chat(@RequestBody ChatRequest request) {
    AiChatService.ChatResult result = aiChatService.chat(request.sessionId(), request.message());
    return ApiResponse.ok(Map.of("reply", result.reply(), "sessionId", result.sessionId()));
  }

  /**
   * 获取知识库文档列表
   * 返回所有已发布且未删除的知识库文档
   *
   * @return 知识库文档列表
   */
  @GetMapping("/knowledge")
  public ApiResponse<List<KnowledgeDocument>> listKnowledge() {
    return ApiResponse.ok(documents.findByPublishedTrueAndDeletedFalseOrderByCreatedAtDesc());
  }

  /**
   * 创建知识库文档（管理端使用）
   * 创建成功后自动生成向量嵌入，清空答案缓存
   *
   * @param doc 知识库文档对象
   * @return 保存后的文档对象
   */
  @PostMapping("/admin/knowledge")
  public ApiResponse<KnowledgeDocument> createKnowledge(@RequestBody KnowledgeDocument doc) {
    generateAndSaveEmbedding(doc);
    KnowledgeDocument saved = documents.save(doc);
    aiChatService.clearCache();
    return ApiResponse.ok(saved);
  }

  /**
   * 批量重新生成所有知识库文档的向量嵌入
   * 当嵌入模型升级或需要重建索引时调用
   *
   * @return 重新生成的文档数量
   */
  @PostMapping("/admin/knowledge/rebuild-embeddings")
  public ApiResponse<Map<String, Object>> rebuildEmbeddings() {
    List<KnowledgeDocument> allDocs = documents.findByPublishedTrueAndDeletedFalseOrderByCreatedAtDesc();
    int successCount = 0;
    int failCount = 0;
    for (KnowledgeDocument doc : allDocs) {
      try {
        generateAndSaveEmbedding(doc);
        documents.save(doc);
        successCount++;
      } catch (Exception e) {
        LoggerFactory.getLogger(AiChatController.class).warn("文档向量生成失败: {} - {}", doc.getTitle(), e.getMessage());
        failCount++;
      }
    }
    aiChatService.clearCache();
    return ApiResponse.ok(Map.of("total", allDocs.size(), "success", successCount, "failed", failCount));
  }

  /**
   * 为文档生成向量嵌入并设置到文档对象
   */
  private void generateAndSaveEmbedding(KnowledgeDocument doc) {
    if (doc.getContent() != null && !doc.getContent().isEmpty()) {
      String fullText = (doc.getTitle() != null ? doc.getTitle() + " " : "") + doc.getContent();
      double[] vector = embeddingService.generate(fullText);
      doc.setEmbedding(embeddingService.toJson(vector));
    }
  }

  /**
   * 对话请求参数
   *
   * @param message 用户当前输入的消息
   * @param sessionId 会话标识（首次为空，后续携带以维持上下文）
   */
  public record ChatRequest(String message, String sessionId) {}
}