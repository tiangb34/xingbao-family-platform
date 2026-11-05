package cn.xingbao.service;

import cn.xingbao.domain.ChildProfile;
import cn.xingbao.domain.GrowthRecord;
import cn.xingbao.domain.KnowledgeDocument;
import cn.xingbao.domain.TrainingTask;
import cn.xingbao.repo.ChildRepository;
import cn.xingbao.repo.GrowthRecordRepository;
import cn.xingbao.repo.KnowledgeDocumentRepository;
import cn.xingbao.repo.TrainingTaskRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Agent 工具注册中心
 *
 * Phase 1 + Phase 2 完整 5 个工具：
 * ① get_child_profile      获取孩子档案摘要（读）
 * ② list_training_tasks    列出已在执行的训练任务（读）
 * ③ search_knowledge        知识库语义搜索（读）
 * ④ get_growth_summary     最近 N 天成长记录汇总（读）
 * ⑤ create_training_tasks   批量创建训练任务（写）
 *
 * 设计要点：
 * - 每个工具都返回"摘要"而非全量原始数据（省 token）
 * - create_training_tasks 的 description 里写清"用户明确要求才调用"（写操作护栏）
 * - toolDefinitions() 返回豆包 Function Calling 的 tools 参数值
 * - invoke() 统一路由：switch-case 按 toolName 分发到具体实现
 */
@Component
public class AgentTools {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AgentTools.class);

  private final ChildRepository childRepo;
  private final TrainingTaskRepository taskRepo;
  private final KnowledgeDocumentRepository docRepo;
  private final GrowthRecordRepository recordRepo;
  private final EmbeddingService embeddingService;
  private final ObjectMapper mapper = new ObjectMapper();

  public AgentTools(ChildRepository childRepo,
                    TrainingTaskRepository taskRepo,
                    KnowledgeDocumentRepository docRepo,
                    GrowthRecordRepository recordRepo,
                    EmbeddingService embeddingService) {
    this.childRepo = childRepo;
    this.taskRepo = taskRepo;
    this.docRepo = docRepo;
    this.recordRepo = recordRepo;
    this.embeddingService = embeddingService;
  }

  // ─────────────────────────────────────────────
  // 工具注册：给 LLM 看的元数据（JSON Schema）
  // ─────────────────────────────────────────────

  /**
   * 返回所有工具的 JSON Schema 列表
   *
   * 这个返回值会作为豆包 Function Calling 请求中的 tools 参数原样发给 LLM。
   * LLM 并不会"读代码"，它靠每个工具的 name + description + parameters 来判断：
   *   - 这个工具是干什么的（description）
   *   - 需要传哪些参数、参数什么类型（parameters）
   *   - 什么时候该用它（description 里写明的使用条件）
   *
   * 因此 description 的措辞直接决定工具调用准确率——写得好，LLM 选对工具；
   * 写得含糊，LLM 就会乱调或漏调。写操作的 description 里尤其要强调调用条件。
   */
  public List<Map<String, Object>> toolDefinitions() {
    List<Map<String, Object>> tools = new ArrayList<>();
    tools.add(getChildProfileDef());      // ① 档案摘要（读）
    tools.add(listTrainingTasksDef());    // ② 已有任务（读）
    tools.add(searchKnowledgeDef());      // ③ 知识库检索（读）
    tools.add(getGrowthSummaryDef());     // ④ 成长汇总（读）
    tools.add(createTrainingTasksDef());  // ⑤ 创建任务（写，有护栏）
    return tools;
  }

  private Map<String, Object> getChildProfileDef() {
    Map<String, Object> params = Map.of(
      "type", "object",
      "properties", Map.of(
        "childId", Map.of("type", "integer", "description", "孩子档案 ID")
      ),
      "required", List.of("childId")
    );
    return Map.of(
      "type", "function",
      "function", Map.of(
        "name", "get_child_profile",
        "description", "获取指定孩子的基本档案信息（昵称、性别、年龄、干预年限）。用于了解孩子的基础情况后再推荐干预计划。",
        "parameters", params
      )
    );
  }

  private Map<String, Object> listTrainingTasksDef() {
    Map<String, Object> params = Map.of(
      "type", "object",
      "properties", Map.of(
        "childId", Map.of("type", "integer", "description", "孩子档案 ID")
      ),
      "required", List.of("childId")
    );
    return Map.of(
      "type", "function",
      "function", Map.of(
        "name", "list_training_tasks",
        "description", "列出指定孩子当前正在执行的训练任务列表。用于避免推荐与已有任务重复的干预内容。",
        "parameters", params
      )
    );
  }

  private Map<String, Object> searchKnowledgeDef() {
    Map<String, Object> params = Map.of(
      "type", "object",
      "properties", Map.of(
        "query", Map.of("type", "string", "description", "要搜索的干预主题，如 '4岁语言发育训练'、'ABA回合教学'")
      ),
      "required", List.of("query")
    );
    return Map.of(
      "type", "function",
      "function", Map.of(
        "name", "search_knowledge",
        "description", "在平台知识库中搜索自闭症干预相关的专业内容（ABA、语言训练、社交技能等），返回最相关的方法摘要。",
        "parameters", params
      )
    );
  }

  private Map<String, Object> getGrowthSummaryDef() {
    Map<String, Object> params = Map.of(
      "type", "object",
      "properties", Map.of(
        "childId", Map.of("type", "integer", "description", "孩子档案 ID"),
        "days", Map.of("type", "integer", "description", "统计最近多少天的数据，默认 7")
      ),
      "required", List.of("childId")
    );
    return Map.of(
      "type", "function",
      "function", Map.of(
        "name", "get_growth_summary",
        "description", "获取指定孩子最近 N 天的成长记录汇总（打卡次数、总时长、按类型分布）。用于评估当前干预的执行情况。",
        "parameters", params
      )
    );
  }

  private Map<String, Object> createTrainingTasksDef() {
    Map<String, Object> taskItem = Map.of(
      "type", "object",
      "properties", Map.of(
        "title", Map.of("type", "string", "description", "训练任务名称，要具体可执行，如 '每日PECS图片卡练习'"),
        "category", Map.of("type", "string", "description", "训练分类：ABA / 语言 / 社交 / 自理 / 精细动作 等"),
        "dailyTarget", Map.of("type", "number", "description", "每日目标量，默认 1"),
        "dailyUnit", Map.of("type", "string", "description", "计量单位：DAY(次数) 或 HOUR(小时)，默认 DAY")
      ),
      "required", List.of("title")
    );
    Map<String, Object> params = Map.of(
      "type", "object",
      "properties", Map.of(
        "childId", Map.of("type", "integer", "description", "孩子档案 ID"),
        "tasks", Map.of(
          "type", "array",
          "description", "要创建的训练任务列表（建议不超过 5 个）",
          "items", taskItem
        )
      ),
      "required", List.of("childId", "tasks")
    );
    return Map.of(
      "type", "function",
      "function", Map.of(
        "name", "create_training_tasks",
        "description", "为指定孩子批量创建训练任务。⚠️ 只有当用户明确要求'帮我创建任务'、'加入计划'、'帮我加上这些'等类似写入请求时才调用此工具。如果用户只是想看看计划但没说要创建，不要调用此工具。",
        "parameters", params
      )
    );
  }

  // ─────────────────────────────────────────────
  // 统一执行入口
  // ─────────────────────────────────────────────

  /**
   * 工具调用路由 —— Agent 执行工具的唯一入口
   *
   * 调用链：AgentService 解析出 LLM 的 tool_call（name + arguments）→ 调到这里 →
   *        这里按 name 分发到具体实现 → 返回 JSON 字符串作为 observation 回填给 LLM。
   *
   * Phase 4-2: 增加 userId 参数，工具层二次校验 childId 归属
   * （AgentController 已在入口校验过一次，这里是防御性编程：
   *   LLM 可能在 arguments 里"幻觉"出不存在的 childId，或用户诱导它查别人的档案）
   *
   * @param toolName 工具名（LLM 返回的 function.name，必须与注册名完全一致）
   * @param arguments LLM 返回的 JSON 参数（未校验，可能缺字段/类型不对）
   * @param userId 当前用户 ID（用于 childId 归属二次校验）
   * @return 执行结果（JSON 字符串，塞回给 LLM 作为 observation）
   */
  public String invoke(String toolName, JsonNode arguments, Long userId) {
    try {
      // ── 安全护栏（第 4 层）：所有带 childId 的工具，先确认这个档案属于当前用户 ──
      // 位置很关键：必须在 switch 分发之前执行，任何工具都绕不过这道门
      if (arguments.has("childId") && !arguments.path("childId").isNull()) {
        long childId = arguments.path("childId").asLong();
        Optional<ChildProfile> child = childRepo.findById(childId);
        if (child.isEmpty() || !child.get().getUserId().equals(userId)) {
          log.warn("[AgentTools] childId 归属校验失败: childId={}, userId={}", childId, userId);
          // 返回错误 JSON 而不是抛异常：让 LLM 知道"越权了"，可以自行调整策略
          return mapper.writeValueAsString(Map.of("error", "无权访问该档案 childId=" + childId));
        }
      }

      // ── 按工具名分发（手写 switch，等价于框架里的 Tool 反射调用）──
      // 注意 default 分支：LLM 偶尔会编造不存在的工具名，这里返回 unknown 让它自己纠正
      return switch (toolName) {
        case "get_child_profile" -> invokeGetChildProfile(arguments);
        case "list_training_tasks" -> invokeListTrainingTasks(arguments);
        case "search_knowledge" -> invokeSearchKnowledge(arguments);
        case "get_growth_summary" -> invokeGetGrowthSummary(arguments);
        case "create_training_tasks" -> invokeCreateTrainingTasks(arguments);
        default -> mapper.writeValueAsString(Map.of("error", "unknown tool: " + toolName));
      };
    } catch (Exception e) {
      // 单个工具出错不能拖垮整个 Agent：包成 JSON 错误返回，LLM 仍可继续推理
      return "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
    }
  }

  // ─────────────────────────────────────────────
  // 具体工具实现（返回摘要，省 token）
  // ─────────────────────────────────────────────

  private String invokeGetChildProfile(JsonNode args) throws Exception {
    long childId = args.path("childId").asLong();
    Optional<ChildProfile> opt = childRepo.findById(childId);
    if (opt.isEmpty()) {
      return mapper.writeValueAsString(Map.of("error", "找不到档案 childId=" + childId));
    }
    ChildProfile c = opt.get();
    int ageYears = 0;
    if (c.getBirthday() != null) {
      long ageDays = ChronoUnit.DAYS.between(c.getBirthday(), LocalDate.now());
      ageYears = (int) (ageDays / 365);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("childId", c.getId());
    result.put("nickname", c.getNickname());
    result.put("gender", c.getGender());
    result.put("ageYears", ageYears);
    result.put("interventionYears", c.getInterventionYears());
    result.put("diagnosis", c.getDiagnosisCiphertext() != null ? "已记录" : "未记录");
    return mapper.writeValueAsString(result);
  }

  private String invokeListTrainingTasks(JsonNode args) throws Exception {
    long childId = args.path("childId").asLong();
    List<TrainingTask> tasks = taskRepo.findByChildIdAndDeletedFalseOrderByCreatedAtDesc(childId);

    var summary = tasks.stream()
      .filter(TrainingTask::isEnabled)
      .map(t -> Map.<String, Object>of(
        "id", t.getId(),
        "title", t.getTitle(),
        "category", Optional.ofNullable(t.getCategory()).orElse("未分类"),
        "dailyTarget", t.getDailyTarget(),
        "dailyUnit", t.getDailyUnit()
      ))
      .toList();

    Map<String, Object> result = Map.of("totalActive", summary.size(), "tasks", summary);
    return mapper.writeValueAsString(result);
  }

  /**
   * ③ search_knowledge —— 知识库检索
   *
   * 优先走向量检索（语义匹配），失败降级为关键词匹配（字面匹配）。
   * 这是复用既有 RAG 能力给 Agent 当工具用的典型做法。
   */
  private String invokeSearchKnowledge(JsonNode args) throws Exception {
    String query = args.path("query").asText();
    List<KnowledgeDocument> all = docRepo.findByPublishedTrueAndDeletedFalseOrderByCreatedAtDesc();

    // 知识库为空：直接返回空结果，避免无意义的 embedding 调用（省 token/费用）
    if (all.isEmpty()) {
      return mapper.writeValueAsString(Map.of("total", 0, "results", List.of()));
    }

    // 优先用向量检索，失败降级为关键词匹配
    List<KnowledgeDocument> top5;
    try {
      // ── 主路径：语义检索 ──────────────────────────────────────
      // 把用户 query 也转成向量，再与每篇文档向量算余弦相似度取 Top5
      // 排序用负数：因为排序是升序，取负后"相似度高"的会排到前面
      double[] qVec = embeddingService.generate(query);
      top5 = all.stream()
        .filter(d -> d.getContent() != null && d.getEmbedding() != null)
        .sorted(Comparator.comparingDouble(d -> {
          try {
            return -embeddingService.cosineSimilarity(qVec, embeddingService.fromJson(d.getEmbedding()));
          } catch (Exception e) { return 0.0; }
        }))
        .limit(5).toList();
    } catch (Exception e) {
      // ── 降级路径：关键词匹配 ──────────────────────────────────
      // embedding 服务不可用时退化为标题/正文包含匹配，保证 Agent 不整体失效
      String lower = query.toLowerCase();
      top5 = all.stream()
        .filter(d -> d.getContent() != null)
        .sorted(Comparator.comparingInt(d -> {
          String title = (d.getTitle() == null ? "" : d.getTitle().toLowerCase());
          String content = d.getContent().toLowerCase();
          // 标题命中权重 10，正文命中权重 1；同样用负号让高分排前
          return -(title.contains(lower) ? 10 : 0) - (content.contains(lower) ? 1 : 0);
        }))
        .limit(5).toList();
    }

    // 摘要截断：每篇最多 500 字（省 token，同时保证 LLM 有足够信息做决策）
    List<Map<String, String>> results = top5.stream().map(d -> {
      String content = d.getContent() == null ? "" : d.getContent();
      if (content.length() > 500) content = content.substring(0, 500) + "...";
      return Map.of(
        "title", d.getTitle() == null ? "无标题" : d.getTitle(),
        "category", d.getCategory() == null ? "" : d.getCategory(),
        "summary", content
      );
    }).toList();

    return mapper.writeValueAsString(Map.of("total", results.size(), "results", results));
  }

  /**
   * ④ get_growth_summary —— 最近 N 天成长记录汇总
   *
   * 关键设计：不返回原始打卡明细（几十条会烧掉大量 token），
   * 而是按 type 聚合成 {次数, 总时长}，让 LLM 直接拿到"哪类练得多、哪类练得少"。
   */
  private String invokeGetGrowthSummary(JsonNode args) throws Exception {
    long childId = args.path("childId").asLong();
    // days 为可选参数：LLM 不传时默认统计最近 7 天
    int days = args.has("days") && !args.path("days").isNull() ? args.path("days").asInt() : 7;

    LocalDate cutoffDate = LocalDate.now().minusDays(days);
    LocalDateTime cutoffTime = cutoffDate.atStartOfDay();

    // 先按时间窗口过滤：只保留 occurredAt >= cutoffTime 的记录
    var records = recordRepo.findByChildIdAndDeletedFalseOrderByOccurredAtDesc(childId).stream()
      .filter(r -> r.getOccurredAt() != null && !r.getOccurredAt().isBefore(cutoffTime))
      .toList();

    // 按 type 汇总：type -> [count, totalMinutes]
    // long[2] 用下标 0 存次数、下标 1 存累计分钟数，避免为每类建临时对象
    Map<String, long[]> byType = new LinkedHashMap<>();
    for (GrowthRecord r : records) {
      String type = r.getType() == null ? "unknown" : r.getType();
      byType.computeIfAbsent(type, k -> new long[2]);
      byType.get(type)[0]++;
      if (r.getDurationMinutes() != null) {
        byType.get(type)[1] += r.getDurationMinutes();
      }
    }

    List<Map<String, Object>> typeSummary = byType.entrySet().stream()
      .map(e -> Map.<String, Object>of(
        "type", e.getKey(),
        "count", e.getValue()[0],
        "totalMinutes", e.getValue()[1]
      ))
      .toList();

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("periodDays", days);
    result.put("recordCount", records.size());
    result.put("byType", typeSummary);
    return mapper.writeValueAsString(result);
  }

  /**
   * ⑤ create_training_tasks —— 批量创建训练任务（唯一写操作）
   *
   * 注意：本方法内部不做"用户是否有写入意图"的判断——
   * 那道护栏在 AgentService（执行前用 hasWriteIntent 关键词检查）。
   * 这里只负责"参数合法就落库"，职责单一，便于复用与测试。
   */
  private String invokeCreateTrainingTasks(JsonNode args) throws Exception {
    long childId = args.path("childId").asLong();
    JsonNode tasksNode = args.path("tasks");
    int created = 0;
    List<String> titles = new ArrayList<>();

    if (tasksNode.isArray()) {
      for (JsonNode item : tasksNode) {
        var task = new TrainingTask();
        task.setChildId(childId);
        task.setTitle(item.path("title").asText());
        if (item.has("category") && !item.path("category").isNull()) {
          task.setCategory(item.path("category").asText());
        }
        if (item.has("dailyTarget") && !item.path("dailyTarget").isNull()) {
          task.setDailyTarget(BigDecimal.valueOf(item.path("dailyTarget").asDouble()));
        }
        if (item.has("dailyUnit") && !item.path("dailyUnit").isNull()) {
          String unit = item.path("dailyUnit").asText();
          task.setDailyUnit(List.of("HOUR", "DAY").contains(unit) ? unit : "DAY");
        }
        taskRepo.save(task);
        titles.add(task.getTitle());
        created++;
      }
    }

    Map<String, Object> result = Map.of(
      "created", created,
      "childId", childId,
      "titles", titles
    );
    return mapper.writeValueAsString(result);
  }

  private String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
  }
}
