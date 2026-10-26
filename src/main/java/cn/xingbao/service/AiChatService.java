package cn.xingbao.service;

import cn.xingbao.domain.Conversation;
import cn.xingbao.domain.ConversationMessage;
import cn.xingbao.domain.KnowledgeDocument;
import cn.xingbao.repo.ConversationMessageRepository;
import cn.xingbao.repo.ConversationRepository;
import cn.xingbao.repo.KnowledgeDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 对话服务
 * 基于 RAG（检索增强生成）实现知识库问答
 * 支持通义千问和豆包两种大模型
 * 支持服务端会话管理，实现多轮对话上下文维护
 */
@Service
public class AiChatService {
  private final KnowledgeDocumentRepository documents;
  private final ConversationRepository conversationRepository;
  private final ConversationMessageRepository messageRepository;
  private final EmbeddingService embeddingService;

  /** 保留的最近消息条数（用于截断过长历史） */
  private static final int MAX_HISTORY_MESSAGES = 20;

  /** HTTP 客户端，配置了连接和读取超时 */
  private final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .build();
  private final ObjectMapper mapper = new ObjectMapper();

  /** 通义千问 API Key */
  @Value("${ai.qwen.api-key:}")
  private String qwenApiKey;

  /** 通义千问 API 地址 */
  @Value("${ai.qwen.url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
  private String qwenUrl;

  /** 通义千问模型名称 */
  @Value("${ai.qwen.model:qwen-plus}")
  private String qwenModel;

  /** 豆包 API Key */
  @Value("${ai.doubao.api-key:}")
  private String doubaoApiKey;

  /** 豆包 API 地址 */
  @Value("${ai.doubao.url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
  private String doubaoUrl;

  /** 豆包模型名称 */
  @Value("${ai.doubao.model:}")
  private String doubaoModel;

  /** 默认使用的 AI 提供商（qwen 或 doubao） */
  @Value("${ai.default-provider:qwen}")
  private String defaultProvider;

  /** 答案缓存：key = sessionId + ":" + userMessage, value = AI 回复 */
  private final ConcurrentHashMap<String, String> answerCache = new ConcurrentHashMap<>();

  /**
   * 清空答案缓存
   * 当知识库文档发生变更时调用，确保缓存答案不会过时
   */
  public void clearCache() {
    int size = answerCache.size();
    answerCache.clear();
    org.slf4j.LoggerFactory.getLogger(AiChatService.class).info("答案缓存已清空，共清除 {} 条缓存", size);
  }

  public AiChatService(KnowledgeDocumentRepository documents,
                        ConversationRepository conversationRepository,
                        ConversationMessageRepository messageRepository,
                        EmbeddingService embeddingService) {
    this.documents = documents;
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.embeddingService = embeddingService;
  }

  /**
   * 核心对话方法 - RAG 流程 + 服务端会话管理
   * 1. 根据 sessionId 获取或创建会话
   * 2. 检索知识库中与用户问题相关的文档
   * 3. 构建包含知识库内容的 System Prompt
   * 4. 调用大模型生成回答
   * 5. 保存用户消息和 AI 回复到数据库
   *
   * @param sessionId 会话标识（首次为空时自动生成）
   * @param userMessage 用户输入的消息
   * @return AI 生成的回复内容和 sessionId
   */
  public ChatResult chat(String sessionId, String userMessage) {
    // 步骤0：获取或创建会话
    Conversation conversation = getOrCreateConversation(sessionId, userMessage);

    // 步骤0.5：检查答案缓存（同一会话 + 相同问题 → 直接返回）
    String cacheKey = conversation.getSessionId() + ":" + userMessage;
    String cachedReply = answerCache.get(cacheKey);
    if (cachedReply != null) {
      conversation.setLastActiveAt(Instant.now());
      conversationRepository.save(conversation);
      return new ChatResult(conversation.getSessionId(), cachedReply);
    }

    // 步骤1：检索相关知识库文档
    List<KnowledgeDocument> relevantDocs = searchKnowledge(userMessage);
    String context = buildContext(relevantDocs);

    // 步骤2：构建 System Prompt，包含角色设定和知识库上下文
    String systemPrompt = """
      你是星宝家庭互助平台的智能助手，专注于自闭症家庭干预和康复知识问答。
      请基于以下知识库内容回答用户问题。如果知识库中没有相关信息，请明确告知用户，并给出一般性建议。
      回答要温和、专业、易懂，避免使用过于专业的医学术语。

      【知识库内容】
      %s

      【回答要求】
      1. 优先基于知识库内容回答
      2. 如果知识库内容不足，可以补充一般性建议，但要说明"以下建议仅供参考"
      3. 回答要简洁明了，通常不超过300字
      4. 必要时可以引用知识库中的具体方法或步骤
      """.formatted(context);

    // 步骤3：从数据库加载最近 N 条历史消息
    List<Map<String, String>> historyMessages = loadRecentHistory(conversation.getId());

    // 步骤4：调用大模型
    String reply = callModel(systemPrompt, userMessage, historyMessages);

    // 步骤5：保存用户消息和 AI 回复到数据库
    saveMessage(conversation.getId(), "user", userMessage);
    saveMessage(conversation.getId(), "assistant", reply);

    // 步骤5.5：存入答案缓存
    answerCache.put(cacheKey, reply);

    // 更新会话活跃时间
    conversation.setLastActiveAt(Instant.now());
    conversationRepository.save(conversation);

    return new ChatResult(conversation.getSessionId(), reply);
  }

  /**
   * 获取或创建会话
   * 如果 sessionId 为空或不存在，则创建新会话
   */
  private Conversation getOrCreateConversation(String sessionId, String userMessage) {
    if (sessionId != null && !sessionId.isEmpty()) {
      return conversationRepository.findBySessionId(sessionId)
        .orElseGet(() -> createNewConversation(userMessage));
    }
    return createNewConversation(userMessage);
  }

  /**
   * 创建新会话
   * 使用用户的第一条消息作为会话标题（截取前 20 字）
   */
  private Conversation createNewConversation(String firstMessage) {
    Conversation conversation = new Conversation();
    conversation.setSessionId(UUID.randomUUID().toString());
    conversation.setTitle(firstMessage.length() > 20
      ? firstMessage.substring(0, 20) + "..."
      : firstMessage);
    conversation.setLastActiveAt(Instant.now());
    return conversationRepository.save(conversation);
  }

  /**
   * 从数据库加载最近 N 条历史消息
   * 自动截断过长历史，避免超出大模型上下文窗口
   */
  private List<Map<String, String>> loadRecentHistory(Long conversationId) {
    List<ConversationMessage> allMessages = messageRepository
      .findByConversationIdOrderByCreatedAtAsc(conversationId);

    // 只保留最近 MAX_HISTORY_MESSAGES 条
    int start = Math.max(0, allMessages.size() - MAX_HISTORY_MESSAGES);
    List<ConversationMessage> recentMessages = allMessages.subList(start, allMessages.size());

    return recentMessages.stream()
      .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
      .collect(Collectors.toList());
  }

  /**
   * 保存消息到数据库
   */
  private void saveMessage(Long conversationId, String role, String content) {
    ConversationMessage message = new ConversationMessage();
    message.setConversationId(conversationId);
    message.setRole(role);
    message.setContent(content);
    messageRepository.save(message);
  }

  /**
   * 根据配置的提供商调用对应的大模型
   */
  private String callModel(String systemPrompt, String userMessage, List<Map<String, String>> historyMessages) {
    String provider = defaultProvider;
    if ("qwen".equals(provider) && qwenApiKey != null && !qwenApiKey.isEmpty()) {
      return callQwen(systemPrompt, userMessage, historyMessages);
    } else if ("doubao".equals(provider) && doubaoApiKey != null && !doubaoApiKey.isEmpty()) {
      return callDoubao(systemPrompt, userMessage, historyMessages);
    } else {
      return "抱歉，AI 服务暂未配置，请联系管理员。";
    }
  }

  /**
   * 调用通义千问 API
   *
   * @param systemPrompt 系统提示词
   * @param userMessage 用户消息
   * @param historyMessages 历史消息列表
   * @return AI 回复内容
   */
  private String callQwen(String systemPrompt, String userMessage, List<Map<String, String>> historyMessages) {
    try {
      // 构建消息列表：system + history + user
      List<Map<String, String>> messages = new ArrayList<>();
      messages.add(Map.of("role", "system", "content", systemPrompt));
      messages.addAll(historyMessages);
      messages.add(Map.of("role", "user", "content", userMessage));

      // 构建请求体
      Map<String, Object> body = Map.of(
        "model", qwenModel,
        "messages", messages,
        "temperature", 0.7,
        "max_tokens", 1000
      );

      // 发送 HTTP 请求
      Request request = new Request.Builder()
        .url(qwenUrl)
        .addHeader("Authorization", "Bearer " + qwenApiKey)
        .addHeader("Content-Type", "application/json")
        .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
        .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          return "抱歉，AI 服务暂时不可用，请稍后再试。";
        }
        String responseBody = response.body().string();
        JsonNode root = mapper.readTree(responseBody);
        return root.path("choices").get(0).path("message").path("content").asText();
      }
    } catch (Exception e) {
      return "抱歉，AI 服务调用出现异常：" + e.getMessage();
    }
  }

  /**
   * 调用豆包 API
   *
   * @param systemPrompt 系统提示词
   * @param userMessage 用户消息
   * @param historyMessages 历史消息列表
   * @return AI 回复内容
   */
  private String callDoubao(String systemPrompt, String userMessage, List<Map<String, String>> historyMessages) {
    try {
      // 构建消息列表：system + history + user
      List<Map<String, String>> messages = new ArrayList<>();
      messages.add(Map.of("role", "system", "content", systemPrompt));
      messages.addAll(historyMessages);
      messages.add(Map.of("role", "user", "content", userMessage));

      // 构建请求体
      Map<String, Object> body = Map.of(
        "model", doubaoModel,
        "messages", messages,
        "temperature", 0.7,
        "max_tokens", 1000
      );

      // 发送 HTTP 请求
      Request request = new Request.Builder()
        .url(doubaoUrl)
        .addHeader("Authorization", "Bearer " + doubaoApiKey)
        .addHeader("Content-Type", "application/json")
        .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.parse("application/json")))
        .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          return "抱歉，AI 服务暂时不可用，请稍后再试。";
        }
        String responseBody = response.body().string();
        JsonNode root = mapper.readTree(responseBody);
        return root.path("choices").get(0).path("message").path("content").asText();
      }
    } catch (Exception e) {
      return "抱歉，AI 服务调用出现异常：" + e.getMessage();
    }
  }

  /**
   * 知识库检索 - 基于向量相似度
   * 将用户查询转为向量，与所有文档向量计算余弦相似度，返回 Top 5
   *
   * @param query 用户查询内容
   * @return 相关性最高的文档列表（最多5篇）
   */
  private List<KnowledgeDocument> searchKnowledge(String query) {
    List<KnowledgeDocument> allDocs = documents.findByPublishedTrueAndDeletedFalseOrderByCreatedAtDesc();

    // 生成查询向量
    double[] queryVector;
    try {
      queryVector = embeddingService.generate(query);
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(AiChatService.class).warn("向量生成失败，降级为关键词检索: {}", e.getMessage());
      return searchKnowledgeFallback(query, allDocs);
    }

    // 计算每篇文档的向量相似度
    return allDocs.stream()
      .filter(doc -> doc.getContent() != null && !doc.getContent().isEmpty() && doc.getEmbedding() != null)
      .sorted(Comparator.comparingDouble(doc -> {
        try {
          double[] docVector = embeddingService.fromJson(doc.getEmbedding());
          return -embeddingService.cosineSimilarity(queryVector, docVector);
        } catch (Exception e) {
          return 0.0;
        }
      }))
      .limit(5)
      .collect(Collectors.toList());
  }

  /**
   * 降级方案：向量检索失败时使用关键词匹配
   */
  private List<KnowledgeDocument> searchKnowledgeFallback(String query, List<KnowledgeDocument> allDocs) {
    String lowerQuery = query.toLowerCase();
    return allDocs.stream()
      .filter(doc -> doc.getContent() != null && !doc.getContent().isEmpty())
      .sorted(Comparator.comparingInt(doc -> {
        String content = doc.getContent().toLowerCase();
        String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";
        String tags = doc.getTags() != null ? doc.getTags().toLowerCase() : "";
        int score = 0;
        if (title.contains(lowerQuery)) score += 10;
        if (tags.contains(lowerQuery)) score += 5;
        if (content.contains(lowerQuery)) score += 1;
        for (String keyword : lowerQuery.split("\\s+")) {
          if (title.contains(keyword)) score += 3;
          if (tags.contains(keyword)) score += 2;
          if (content.contains(keyword)) score += 1;
        }
        return -score;
      }))
      .limit(5)
      .collect(Collectors.toList());
  }

  /**
   * 将检索到的文档格式化为上下文文本
   *
   * @param docs 检索到的文档列表
   * @return 格式化后的上下文文本
   */
  private String buildContext(List<KnowledgeDocument> docs) {
    if (docs.isEmpty()) {
      return "（暂无相关知识库内容）";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < docs.size(); i++) {
      KnowledgeDocument doc = docs.get(i);
      sb.append("【文档").append(i + 1).append("】\n");
      sb.append("标题：").append(doc.getTitle()).append("\n");
      if (doc.getCategory() != null) {
        sb.append("分类：").append(doc.getCategory()).append("\n");
      }
      sb.append("内容：").append(doc.getContent()).append("\n\n");
    }
    return sb.toString();
  }

  /**
   * 对话结果
   *
   * @param sessionId 会话标识
   * @param reply AI 回复内容
   */
  public record ChatResult(String sessionId, String reply) {}
}