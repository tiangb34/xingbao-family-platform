package cn.xingbao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 干预计划推荐 Agent 核心服务（Phase 1）
 *
 * 核心实现：
 * 1. ReAct 循环 —— while 循环，maxIterations=10 防止死循环
 * 2. Function Calling —— 首轮带 tools 参数调用豆包，解析 tool_calls，执行后塞回 history
 * 3. System Prompt 拆版 —— 完整版（首轮）+ 精简版（后续轮次）省 token
 * 4. assistant 消息带 tool_calls 字段原样回传 —— 豆包 Function Calling 协议要求
 *
 * 与 AiChatService 的区别：
 * - AiChatService 是单次 RAG 调用（检索知识库 → 拼上下文 → 一次 LLM 调用）
 * - AgentService 是多轮 ReAct 循环（LLM 自主决定调用哪些工具、什么顺序）
 */
@Service
public class AgentService {

  private static final Logger log = LoggerFactory.getLogger(AgentService.class);

  /** ReAct 循环硬保护：最多迭代 10 轮 */
  private static final int MAX_ITERATIONS = 10;

  private final AgentTools agentTools;
  private final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
    .build();
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${ai.doubao.api-key:}")
  private String doubaoApiKey;
  @Value("${ai.doubao.url:https://ark.cn-beijing.volces.com/api/v3/chat/completions}")
  private String doubaoUrl;
  @Value("${ai.doubao.model:doubao-seed-2-1-turbo-260628}")
  private String doubaoModel;

  public AgentService(AgentTools agentTools) {
    this.agentTools = agentTools;
  }

  // ─────────────────────────────────────────────
  // 核心入口：ReAct 主循环
  // ─────────────────────────────────────────────

  /**
   * 根据用户需求生成干预计划
   *
   * @param childId 目标孩子档案 ID
   * @param userId  当前用户 ID（Phase 4: 传递给 AgentTools 二次校验 childId 归属）
   * @param userMessage 用户输入的自然语言需求
   * @return 包含最终答案、迭代次数、执行轨迹的结果
   */
  public AgentResult generatePlan(Long childId, Long userId, String userMessage) {
    // ═══════════════════════════════════════════════════════════════
    // 【第 0 步】初始化对话上下文 messages
    // messages 是 Agent 的"记忆黑板"：LLM 每次只能看到这里的全部内容，
    // 所以"谁说了什么、调了哪个工具、工具返回了什么"都必须写进来。
    // ═══════════════════════════════════════════════════════════════
    List<Map<String, Object>> messages = new ArrayList<>();

    // 首条：system 角色，告诉 LLM 它是谁、要做什么、能用哪些工具、输出什么格式
    // 这里用完整版（约 1500 tokens），因为它是 LLM 第一次"认识任务"，信息必须全
    messages.add(Map.of("role", "system", "content", buildFullSystemPrompt(childId)));
    // 第二条：user 角色，家长的原始诉求
    messages.add(Map.of("role", "user", "content", userMessage));

    List<TraceEntry> trace = new ArrayList<>();
    // toolsPassed 标记位：控制 tools 参数"只在首轮传"
    // 原因：tools 的 JSON Schema 本身就占几百 token，每轮重复传纯属浪费；
    //       LLM 首轮已经记住了工具清单，后续轮次靠 messages 里的历史就能继续调工具
    boolean toolsPassed = false;
    String finalAnswer = null;
    int iteration = 0;

    // ═══════════════════════════════════════════════════════════════
    // 【第 1 步】ReAct 主循环 —— Agent 的心脏
    // 每一轮做三件事：①问 LLM ②LLM 要工具就执行并回填 ③LLM 给答案就收工
    // MAX_ITERATIONS 是硬性保险丝：防止 LLM 陷入"无限调工具"的死循环
    // ═══════════════════════════════════════════════════════════════
    while (iteration < MAX_ITERATIONS) {
      iteration++;
      log.info("[Agent] 第 {} 轮迭代，messages 数={}", iteration, messages.size());

      // ── 1.1 调用 LLM ─────────────────────────────────────────
      // includeTools 只在第 1 轮为 true（!toolsPassed），之后恒为 false
      // 返回值是豆包的原始 JSON 响应，可能含 content，也可能含 tool_calls
      JsonNode response = callDoubao(messages, !toolsPassed);
      toolsPassed = true;

      // LLM 调用失败（网络错误 / API Key 缺失 / 限流）→ 直接退出，别让用户干等
      if (response == null) {
        log.warn("[Agent] LLM 调用失败，终止循环");
        trace.add(new TraceEntry("error", "LLM 调用失败"));
        break;
      }

      // ── 1.2 System Prompt 降级（省 token 的关键操作）────────────
      // 第 1 轮之后，把 messages[0] 的完整版 system 替换成精简版：
      //   完整版 1500 tokens → 精简版 400 tokens，每轮省 1100 tokens
      // 为什么可以这样：任务目标 LLM 已经记住了，后续只需要"继续推理"的简短提示
      if (iteration == 1) {
        messages.set(0, Map.of("role", "system", "content", buildShortSystemPrompt()));
      }

      // ── 1.3 解析 LLM 响应 ────────────────────────────────────
      // 豆包响应结构：choices[0].message.{content, tool_calls}
      //   content    = LLM 的文字输出（思考过程或最终答案）
      //   tool_calls = LLM 决定要调用的工具数组（不存在时该节点为空）
      JsonNode choice = response.path("choices").path(0);
      JsonNode message = choice.path("message");
      JsonNode toolCalls = message.path("tool_calls");
      String content = message.path("content").asText();

      log.info("[Agent] LLM 返回: content={}, hasToolCalls={}",
        truncate(content, 100),
        toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty());

      // ── 1.4 把 assistant 消息回填进 history（协议强制要求）────────
      // ⚠️ 关键点：如果 LLM 返回了 tool_calls，这条 assistant 消息**必须原样带上 tool_calls 字段**
      // 原因：下一轮我们要追加 role=tool 的结果消息，而协议要求 tool 消息必须"配对"到
      //       某个 assistant 消息里的 tool_call id，否则豆包会直接报 400 参数错误
      Map<String, Object> assistantMsg = new LinkedHashMap<>();
      assistantMsg.put("role", "assistant");
      if (content != null && !content.isBlank()) {
        assistantMsg.put("content", content);
      }
      if (toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty()) {
        assistantMsg.put("tool_calls", toolCalls);
      }
      messages.add(assistantMsg);

      // ═══════════════════════════════════════════════════════════
      // 【分支 A】LLM 想要调用工具 → 逐个执行 → 回填结果 → 继续下一轮
      // 这是 ReAct 中"Act"的部分：LLM 的每轮思考都可能产生新的工具调用
      // ═══════════════════════════════════════════════════════════
      if (toolCalls != null && toolCalls.isArray() && !toolCalls.isEmpty()) {
        log.info("[Agent] → 执行 {} 个 tool_call", toolCalls.size());

        // trace 记下本轮 LLM 的思考内容（前端"执行轨迹"里展示的第一类信息）
        trace.add(new TraceEntry("think", content.isEmpty() ? "(决定调用工具)" : content));

        // tool_calls 是数组：LLM 可能一口气要求调多个工具（如同时查档案+查知识库）
        for (JsonNode tc : toolCalls) {
          // 每个 tool_call 三个字段：
          //   id        —— 唯一标识，回填 tool 结果时必须用它配对
          //   name      —— 工具名（对应 AgentTools 里注册的函数名）
          //   arguments —— 参数（JSON 字符串，需自己 readTree 解析）
          String toolCallId = tc.path("id").asText();
          String toolName = tc.path("function").path("name").asText();
          String argumentsStr = tc.path("function").path("arguments").asText();

          log.info("[Agent]   调工具: {}({})", toolName, truncate(argumentsStr, 150));

          // ── 安全护栏（第 3 层）：写操作运行时硬检查 ────────────────
          // 为什么不能只靠 Prompt？因为 LLM 可能误判用户意图（如把"看看计划"理解成"创建计划"）
          // 所以这里在真正执行前，用关键词再次扫描用户原始消息，确认确实有写入意图
          if ("create_training_tasks".equals(toolName)) {
            if (!hasWriteIntent(userMessage)) {
              log.warn("[Agent]   拒绝写操作：用户消息无写入意图关键词，userMessage={}", truncate(userMessage, 100));
              trace.add(new TraceEntry("guard", "拒绝写操作：用户未明确要求创建"));
              // 注意：拒绝不是"偷偷跳过"，而是把拒绝原因作为工具结果塞回给 LLM
              // 这样 LLM 能感知到"我被拒绝了"，从而调整策略（改为只输出建议）
              messages.add(Map.of(
                "role", "tool",
                "tool_call_id", toolCallId,
                "content", "{\"error\": \"写操作被拒绝：用户未明确要求创建训练任务。请只输出干预计划建议，不要调用 create_training_tasks。\"}"
              ));
              continue;
            }
          }

          // ── 执行工具 ─────────────────────────────────────────
          // 交给 AgentTools.invoke() 统一路由（内部会再做一次 childId 归属校验）
          // 工具抛异常时不往外崩，而是把异常包成 JSON 错误返回给 LLM，
          //   让 LLM 有机会自己决策"换个工具试试"或"告诉用户失败了"
          String toolResult;
          try {
            JsonNode args = mapper.readTree(argumentsStr);
            toolResult = agentTools.invoke(toolName, args, userId);
          } catch (Exception e) {
            toolResult = "{\"error\": \"tool execution failed: " + escapeJson(e.getMessage()) + "\"}";
          }

          log.info("[Agent]   工具返回: {}", truncate(toolResult, 150));

          // trace 记下"调了哪个工具、返回了什么"（前端执行轨迹的第二类信息）
          trace.add(new TraceEntry("tool", toolName + " → " + truncate(toolResult, 200)));

          // ── 回填工具结果（ReAct 中"Observation"的部分）─────────────
          // 三条铁律：role 必须是 tool、必须带 tool_call_id（和上面的 id 配对）、content 是结果字符串
          // 这三条缺一不可，否则下一轮请求会被豆包拒绝
          messages.add(Map.of(
            "role", "tool",
            "tool_call_id", toolCallId,
            "content", toolResult
          ));
        }
        // 工具都执行完了，回到 while 顶部：让 LLM 基于新得到的信息继续推理
        continue;
      }

      // ═══════════════════════════════════════════════════════════
      // 【分支 B】LLM 不再请求工具 → 说明信息够了，这就是最终答案
      // ReAct 的终止条件：LLM 主动停止调工具，直接输出文字
      // ═══════════════════════════════════════════════════════════
      // Phase 4-3：extractJson 做兜底，处理 LLM 偶尔加的 markdown 代码块/前后缀
      finalAnswer = extractJson(content);
      trace.add(new TraceEntry("final", finalAnswer));
      log.info("[Agent] 第 {} 轮输出最终答案，循环结束", iteration);
      break;
    }

    // ═══════════════════════════════════════════════════════════════
    // 【第 2 步】循环退出后的兜底处理
    // 走到这里可能有两种情况：① 正常 break（finalAnswer 有值）
    //                        ② 撞到 MAX_ITERATIONS 上限（finalAnswer 仍为 null）
    // 后者给用户一个友好的提示，而不是把"null"或空字符串扔回去
    // ═══════════════════════════════════════════════════════════════
    if (finalAnswer == null) {
      finalAnswer = "抱歉，AI 助手思考超时，请换一种方式描述您的需求再试试。";
      trace.add(new TraceEntry("error", "max iterations reached (" + MAX_ITERATIONS + ")"));
    }

    log.info("[Agent] 本次执行完成: iterations={}", iteration);
    // 返回三样东西：最终答案、实际用了几轮、完整执行轨迹（供前端展示 + 排查问题）
    return new AgentResult(finalAnswer, iteration, trace);
  }

  // ─────────────────────────────────────────────
  // 豆包 API 调用（支持 Function Calling）
  // ─────────────────────────────────────────────

  /**
   * 调用豆包 Chat Completions API（OpenAI 兼容格式）
   *
   * 这是"和 LLM 对话的唯一出口"：Agent 的每一次思考都通过这个方法完成。
   * 请求体关键字段：
   *   model       —— 模型名
   *   messages    —— 完整对话上下文（system / user / assistant / tool 四类角色）
   *   tools       —— 工具清单（JSON Schema），只在首轮传
   *   temperature —— 0.7：干预计划需要一点创造性，但也不能太飘
   *   max_tokens  —— 2000：够输出一份完整 JSON 干预计划
   *
   * @param includeTools 是否携带 tools 参数（首轮 true，后续 false）
   * @return 豆包原始响应 JSON；调用失败返回 null（由调用方决定是否终止循环）
   */
  private JsonNode callDoubao(List<Map<String, Object>> messages, boolean includeTools) {
    try {
      // Key 未配置是开发期最常见的失败原因，先短路返回，避免打出难读的 401
      if (doubaoApiKey == null || doubaoApiKey.isEmpty()) {
        log.warn("[Agent] 豆包 API Key 未配置");
        return null;
      }

      // 用 LinkedHashMap 保证字段顺序（便于阅读日志，虽然对 API 本身无影响）
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", doubaoModel);
      body.put("messages", messages);
      body.put("temperature", 0.7);
      body.put("max_tokens", 2000);

      // tools 参数只在首轮传：
      // 首轮 = LLM 第一次"看到"工具清单，之后它已经记住，重复传纯属烧 token
      if (includeTools) {
        body.put("tools", agentTools.toolDefinitions());
      }

      String bodyJson = mapper.writeValueAsString(body);
      log.info("[Agent] 调用豆包 API: includeTools={}, messagesCount={}", includeTools, messages.size());

      Request request = new Request.Builder()
        .url(doubaoUrl)
        .addHeader("Authorization", "Bearer " + doubaoApiKey)
        .addHeader("Content-Type", "application/json")
        .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
        .build();

      try (Response response = client.newCall(request).execute()) {
        // 非 2xx：把状态码和响应体都打出来，方便排查（限流 / Key 过期 / 参数错误）
        if (!response.isSuccessful()) {
          String err = response.body() != null ? response.body().string() : "no body";
          log.error("[Agent] 豆包 API 失败: code={}, body={}", response.code(), err);
          return null;
        }
        // 返回原始 JSON 树，交由 generatePlan 解析 choices[0].message
        return mapper.readTree(response.body().string());
      }
    } catch (Exception e) {
      // 网络超时 / 解析失败等统一收敛为 null，不让异常中断整个 Agent 流程
      log.error("[Agent] 豆包调用异常", e);
      return null;
    }
  }

  // ─────────────────────────────────────────────
  // System Prompt（Phase 1：完整版 + 精简版）
  // ─────────────────────────────────────────────

  /**
   * 完整版 System Prompt（首轮用）
   *
   * 包含：角色设定 + 5 个工具的引导使用顺序 + 写操作护栏 + 安全边界 + 严格 JSON 输出 Schema
   * 首轮 ~1500 tokens
   */
  private String buildFullSystemPrompt(Long childId) {
    return """
      你是星宝家庭互助平台的干预计划推荐助手，专门帮助自闭症儿童家长制定个性化的居家干预计划。
      当前正在为 childId=%d 的孩子制定计划。

      【你的工作流程】
      按以下顺序调用工具，尽可能在一次回复中同时调用多个工具：
      1. get_child_profile          — 先了解孩子的年龄、性别、干预年限等基础情况
      2. list_training_tasks        — 查看孩子已在执行的训练任务，避免重复推荐
      3. get_growth_summary         — 评估最近 N 天的干预执行情况（哪些类型打卡多、哪些少）
      4. search_knowledge            — 搜索与孩子情况匹配的干预方法（ABA、语言训练、社交技能等）
      5. create_training_tasks       — （可选）如果用户明确要求"帮我创建"、"加入计划"时才调用

      【工具使用原则】
      - 尽可能在一次回复中同时调用多个工具（用 tool_calls 数组），减少往返次数
      - 工具返回的都是摘要，基于摘要做决策即可，不需要再调更多工具
      - 如果已经获取了足够信息（档案 + 已有任务 + 知识方法），直接输出最终 JSON 干预计划
      - ⚠️ create_training_tasks 是写操作，只有当用户明确说"帮我创建任务"、"加入计划"、"帮我加上这些"时才调用
        如果用户只是想看看推荐的计划但没说要保存，绝对不要调用 create_training_tasks

      【安全边界】
      - 你提供的建议仅供家庭参考，不构成医疗诊断或治疗方案
      - 推荐的训练任务名称要具体、可执行（如 "每日 PECS 图片卡练习"，不要泛泛的 "语言训练"）
      - 不要输出任何药物相关建议
      - 不要建议停止或替换专业医生/康复师的干预方案

      【最终输出格式】
      你必须输出严格的 JSON 对象，不要加入任何前后缀文字、markdown 标记或解释说明。
      JSON 结构和字段类型必须与下面完全一致：
      {
        "planSummary": "一句话（20-40字）概括本次干预计划的核心目标，给家长看的，要温暖具体",
        "dailySchedule": [
          { "time": "上午", "task": "训练名称（3-6字，具体可执行）", "details": "具体做法，含时长和频率" }
        ],
        "weeklyFocus": ["必须是纯字符串，每条一句话，如'帮助孩子主动发起交流'，不要嵌套对象"],
        "tools": ["推荐教具名称，如'PECS图片卡'，纯字符串"],
        "notes": "2-3句温馨提醒，用家长能听懂的语气"
      }
      严格约束：
      - weeklyFocus 和 tools 必须是字符串数组，每个元素是纯字符串，绝对不能是对象
      - dailySchedule 中 time 只能是 "上午" / "下午" / "晚上" / "睡前" 之一
      - 所有文字面向家长，语气温暖鼓励，不要用专业术语堆砌
      - dailySchedule 建议 3-6 条，weeklyFocus 建议 3-5 条
      """.formatted(childId);
  }

  /**
   * 精简版 System Prompt（后续轮次用，省 token）
   *
   * 只保留角色 + 继续推理的核心指令 + 写操作护栏 + JSON 输出约束
   * 后续轮次 ~400 tokens
   */
  private String buildShortSystemPrompt() {
    return """
      你是星宝家庭互助平台的干预计划推荐助手。
      基于已获取的工具调用结果，继续推理并决定下一步：
      - 如果还缺信息 → 调用工具补充（尽可能一次调多个）
      - 如果信息已足够 → 输出最终 JSON 干预计划（不要加任何前后缀）

      ⚠️ 写操作护栏：create_training_tasks 只在用户明确说"帮我创建"、"加入计划"时才调用。
      如果用户只是想看看计划，绝对不要调写工具。

      输出约束：严格 JSON，包含 planSummary / dailySchedule / weeklyFocus / tools / notes 字段。
      weeklyFocus 和 tools 必须是纯字符串数组（不要嵌套对象）。dailySchedule 中 time 只能是 上午/下午/晚上/睡前。
      所有文字面向家长，温暖鼓励，不要加 markdown 或解释文字。
      """;
  }

  // ─────────────────────────────────────────────
  // 工具方法
  // ─────────────────────────────────────────────

  private String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max) + "...";
  }

  private String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
  }

  // ─────────────────────────────────────────────
  // Phase 4 安全辅助方法
  // ─────────────────────────────────────────────

  /**
   * 判断用户消息里是否有明确的"写入意图"关键词
   * Phase 4-1 写操作运行时硬检查：不只是靠 Prompt 约束 LLM，
   * 而是在执行写工具前再次看用户原始消息
   */
  private boolean hasWriteIntent(String userMessage) {
    if (userMessage == null || userMessage.isBlank()) return false;
    String lower = userMessage.toLowerCase();
    // 写入意图关键词（覆盖常见说法）
    String[] writeKeywords = {
      "创建", "加入", "保存", "加到", "加上", "帮我做", "帮我加上",
      "帮我创建", "帮我加入", "帮我保存", "保存到", "存到", "写入",
      "add", "create", "save", "include", "加到计划", "加入计划"
    };
    for (String kw : writeKeywords) {
      if (lower.contains(kw.toLowerCase())) return true;
    }
    return false;
  }

  /**
   * 从 LLM 输出里提取 JSON（Phase 4-3 JSON 兜底）
   * LLM 可能输出：
   * - 合法 JSON：{"planSummary": "..."}  → 原样返回
   * - markdown 代码块：```json\n{...}\n```  → 提取花括号内
   * - 带前后缀：好的，这是建议：\n{...}  → 提取第一个 { 到最后一个 }
   * - 完全无 JSON → 原样返回（调用方自行处理）
   */
  private String extractJson(String llmOutput) {
    if (llmOutput == null || llmOutput.isBlank()) return llmOutput;

    // 尝试 1: 直接 parse
    try {
      mapper.readTree(llmOutput);
      return llmOutput;  // 已经是合法 JSON
    } catch (Exception ignored) {}

    // 尝试 2: 找第一个 { 到最后一个 }
    int firstBrace = llmOutput.indexOf('{');
    int lastBrace = llmOutput.lastIndexOf('}');
    if (firstBrace >= 0 && lastBrace > firstBrace) {
      String extracted = llmOutput.substring(firstBrace, lastBrace + 1);
      try {
        mapper.readTree(extracted);
        return extracted;
      } catch (Exception ignored) {}
    }

    // 尝试 3: 去掉可能存在的 markdown 代码块标记
    String cleaned = llmOutput
      .replaceAll("(?s)^```json\\s*", "")
      .replaceAll("(?s)\\s*```$", "")
      .trim();
    try {
      mapper.readTree(cleaned);
      return cleaned;
    } catch (Exception ignored) {}

    // 兜底：原样返回，让调用方自行 parse
    log.warn("[Agent] LLM 输出无法解析为 JSON，原样返回，输出前 100 字: {}", truncate(llmOutput, 100));
    return llmOutput;
  }

  // ─────────────────────────────────────────────
  // 内部数据结构
  // ─────────────────────────────────────────────

  /** Agent 执行结果 */
  public record AgentResult(String answer, int iterations, List<TraceEntry> trace) {}

  /** 执行轨迹条目（给前端/日志看 Agent 的思考过程） */
  public record TraceEntry(String type, String detail) {}
}
