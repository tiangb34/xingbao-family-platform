# 干预计划推荐 Agent 设计方案

> 文档版本：v1.0　|　最后更新：2026-11-04　|　作者：开发阶段初稿

## 一、设计目标与范围

### 目标

在星宝家庭互助平台中新增一个 AI Agent，它能：**根据自闭症孩子的档案、已有训练任务、成长记录、知识库内容，自主调用多个工具，经过多步推理，最终输出个性化的每日干预计划**。

### 为什么是 Agent 不是普通 RAG

| 对比项 | 普通 RAG 聊天（已有） | 本方案 Agent |
|---|---|---|
| LLM 调用次数 | 1 次 | 3-6 次（ReAct 循环） |
| 能做什么 | 基于知识库问答 | 自主决定查什么数据、按什么顺序、综合分析后做决策 |
| 用户体验 | "你问我答" | "你告诉我需求 → 我自动把所有信息查好 → 给你完整方案" |
| 写操作 | ❌ 只能读 | ✅ 可创建训练任务（经用户明确要求） |
| 核心技术点 | 向量检索、Prompt 工程 | Function Calling、ReAct、多工具编排、安全边界 |

### 边界（不做什么）

- ❌ 不做医疗诊断 / 治疗方案（和平台既有免责声明一致）
- ❌ 不引入 LangChain、Spring AI 等框架（纯手写，学底层机制）
- ❌ 不做前端 UI 改动（先把后端 Agent 跑通）
- ❌ 不修改任何现有文件（完全新增，零侵入）

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         AgentController                         │
│  POST /api/v1/agent/plan                                        │
│  输入: { childId, userId, userMessage }                         │
│  输出: { plan, iterations, trace }                              │
│  职责: 安全校验（childId 归属 + 隐私同意）                       │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                          AgentService                           │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    ReAct 主循环                            │  │
│  │                                                           │  │
│  │  while iteration < MAX_ITERATIONS(10):                    │  │
│  │    1. 调用 LLM → 拿到 assistant response                  │  │
│  │    2. 如果 response 有 tool_calls:                         │  │
│  │       → 遍历每个 tool_call → 调 AgentTools.invoke()        │  │
│  │       → 把结果追加 messages → 继续循环                      │  │
│  │    3. 如果 response 是纯 content（无 tool_calls）:        │  │
│  │       → 这就是最终答案 → break                              │  │
│  │                                                           │  │
│  │  循环退出后返回: { answer, iterations, trace }            │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  LLM 调用: 豆包 API（doubao-seed-2-1-turbo）                     │
│  关键参数: messages, tools(仅首轮传), temperature=0.7             │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                          AgentTools                             │
│  工具注册中心 + 统一执行路由                                      │
│                                                                 │
│  ┌─ 5 个工具 ─────────────────────────────────────────────┐      │
│  │ ① get_child_profile(childId)  → 档案摘要（读）          │      │
│  │ ② list_training_tasks(childId) → 现有任务列表（读）     │      │
│  │ ③ search_knowledge(query)       → 知识库方法（读）       │      │
│  │ ④ get_growth_summary(childId, days) → 成长汇总（读）     │      │
│  │ ⑤ create_training_tasks(childId, tasks) → 创建任务（写） │      │
│  └────────────────────────────────────────────────────────┘      │
│                                                                 │
│  每个工具包含: toolDefinition(JSON Schema for LLM) + invoke()    │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    现有 Repository / Service                     │
│  ChildRepository │ TrainingTaskRepository │ KnowledgeDocument   │
│  GrowthRecordRepository │ EmbeddingService (向量检索)            │
└─────────────────────────────────────────────────────────────────┘
```

### 文件变更清单

| 操作 | 文件路径 | 说明 |
|---|---|---|
| **新增** | `src/main/java/cn/xingbao/service/AgentTools.java` | 工具注册中心 |
| **新增** | `src/main/java/cn/xingbao/service/AgentService.java` | ReAct 主循环 + LLM 调用 |
| **新增** | `src/main/java/cn/xingbao/controller/AgentController.java` | HTTP 端点 + 安全校验 |
| 修改 | 无 | 完全不碰现有文件 |

**不引入任何新 Maven 依赖**（pom.xml 零改动）。

---

## 三、核心：ReAct 主循环伪代码

```java
public AgentResult generatePlan(Long childId, String userMessage) {
    List<Map<String, Object>> messages = new ArrayList<>();
    messages.add(systemPrompt("full", childId));  // 首轮用完整版
    messages.add(userMessage);

    List<TraceEntry> trace = new ArrayList<>();
    boolean toolsPassed = false;
    String finalAnswer = null;

    for (int i = 1; i <= MAX_ITERATIONS(10); i++) {
        JsonNode resp = callDoubao(messages, !toolsPassed);
        toolsPassed = true;
        // 首轮之后把 system 替换成精简版（省 token）
        if (i == 1) messages.set(0, systemPrompt("short", childId));

        JsonNode msg = resp.path("choices").path(0).path("message");
        JsonNode toolCalls = msg.path("tool_calls");
        String content = msg.path("content").asText();

        // 把 assistant 消息塞回 history（带 tool_calls 字段）
        messages.add(assistantMsg(content, toolCalls));

        if (toolCalls 存在 && 非空) {
            // → 执行每个 tool_call，结果追加为 tool role
            trace.add(TraceEntry.think(content));
            for (tc : toolCalls) {
                String result = AgentTools.invoke(tc.name(), tc.args());
                messages.add(toolMsg(tc.id(), result));
                trace.add(TraceEntry.tool(tc.name(), result));
            }
            continue;  // 下一轮 LLM 调用
        }

        // → 无 tool_calls，LLM 给出最终答案
        trace.add(TraceEntry.final_(content));
        finalAnswer = content;
        break;
    }

    if (finalAnswer == null) finalAnswer = "思考超时，请换一种方式描述需求";
    return new AgentResult(finalAnswer, iterations, trace);
}
```

### 关键设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| tools 参数只首轮传 | ✅ | 省 token，多轮对话中 tools 不需要重复 |
| System Prompt 拆两版 | ✅ | 完整版含角色+任务+格式+安全边界；精简版只含角色+继续推理提示 |
| assistant 消息要追加到 history | ✅ | 下一轮 LLM 需要看到上一轮说了什么、决定调哪些工具 |
| assistant 消息带 tool_calls 字段 | ✅ | 豆包 Function Calling 规范：LLM 返回的 tool_calls 必须原样回传 history |
| tool 消息格式 | `{ role: "tool", tool_call_id: "...", content: "..." }` | 标准 Function Calling 协议 |

---

## 四、工具清单（详细）

### 工具 ①：get_child_profile

**给 LLM 看的 JSON Schema**：
```json
{
  "type": "function",
  "function": {
    "name": "get_child_profile",
    "description": "获取孩子档案（年龄/性别/干预年限），用于了解基础情况后再推荐",
    "parameters": {
      "type": "object",
      "properties": {
        "childId": { "type": "integer", "description": "孩子档案 ID" }
      },
      "required": ["childId"]
    }
  }
}
```

**返回值示例（摘要，不是全量数据）**：
```json
{
  "childId": 1,
  "nickname": "小星",
  "gender": "男",
  "ageYears": 4,
  "interventionYears": "2年",
  "diagnosis": "已记录"
}
```

---

### 工具 ②：list_training_tasks

**JSON Schema**：
```json
{
  "type": "function",
  "function": {
    "name": "list_training_tasks",
    "description": "列出孩子当前正在执行的训练任务，避免重复推荐",
    "parameters": {
      "type": "object",
      "properties": {
        "childId": { "type": "integer" }
      },
      "required": ["childId"]
    }
  }
}
```

**返回值示例**：
```json
{
  "totalActive": 2,
  "tasks": [
    { "id": 1, "title": "每日ABA打卡", "category": "ABA", "dailyTarget": 1, "dailyUnit": "DAY" },
    { "id": 2, "title": "PECS图片练习", "category": "语言", "dailyTarget": 3, "dailyUnit": "DAY" }
  ]
}
```

---

### 工具 ③：search_knowledge

**JSON Schema**：
```json
{
  "type": "function",
  "function": {
    "name": "search_knowledge",
    "description": "知识库语义搜索，返回最相关的干预方法摘要",
    "parameters": {
      "type": "object",
      "properties": {
        "query": { "type": "string", "description": "如 '4岁语言发育训练'" }
      },
      "required": ["query"]
    }
  }
}
```

**返回值示例（每篇摘要截断到 300 字，省 token）**：
```json
{
  "total": 3,
  "results": [
    { "title": "PECS图片交换系统", "category": "语言", "summary": "PECS 是一种视觉辅助沟通方法..." },
    { "title": "ABA回合教学", "category": "ABA", "summary": "回合教学由指令、反应、强化三步组成..." },
    { "title": "社交故事法", "category": "社交", "summary": "社交故事用图文描述社交场景..." }
  ]
}
```

**实现逻辑**：复用现有 `EmbeddingService` 做向量余弦相似度 → Top 5 → 失败降级关键词匹配。

---

### 工具 ④：get_growth_summary

**JSON Schema**：
```json
{
  "type": "function",
  "function": {
    "name": "get_growth_summary",
    "description": "最近 N 天成长记录汇总，评估干预执行情况",
    "parameters": {
      "type": "object",
      "properties": {
        "childId": { "type": "integer" },
        "days": { "type": "integer", "description": "默认 7" }
      },
      "required": ["childId"]
    }
  }
}
```

**返回值示例**：
```json
{
  "periodDays": 7,
  "recordCount": 12,
  "byType": [
    { "type": "ABA", "count": 5, "totalMinutes": 150 },
    { "type": "语言", "count": 4, "totalMinutes": 60 },
    { "type": "社交", "count": 3, "totalMinutes": 45 }
  ]
}
```

---

### 工具 ⑤：create_training_tasks（唯一写操作）

**JSON Schema**：
```json
{
  "type": "function",
  "function": {
    "name": "create_training_tasks",
    "description": "批量创建训练任务。只有用户明确要求'帮我创建'或'加入计划'时才调用。",
    "parameters": {
      "type": "object",
      "properties": {
        "childId": { "type": "integer" },
        "tasks": {
          "type": "array",
          "description": "要创建的训练任务列表（建议不超过 5 个）",
          "items": {
            "type": "object",
            "properties": {
              "title": { "type": "string", "description": "训练名称，如 '每日卡片配对练习'" },
              "category": { "type": "string", "description": "训练分类：ABA/语言/社交/自理" },
              "dailyTarget": { "type": "number", "description": "每日目标量，默认 1" },
              "dailyUnit": { "type": "string", "description": "计量单位：DAY(次数) 或 HOUR(小时)" }
            },
            "required": ["title"]
          }
        }
      },
      "required": ["childId", "tasks"]
    }
  }
}
```

**返回值示例**：
```json
{
  "created": 3,
  "childId": 1,
  "titles": ["每日卡片配对练习", "社交故事共读", "精细动作串珠"]
}
```

---

## 五、System Prompt 设计

### 完整版（首轮用，~1200 tokens）

```
你是星宝家庭互助平台的干预计划推荐助手，专门帮助自闭症儿童家长制定个性化的居家干预计划。
当前正在为 childId=%d 的孩子制定计划。

【工作流程】
1. 先 get_child_profile 了解孩子基础情况（年龄、性别、干预年限）
2. list_training_tasks 查已有任务（避免重复推荐）
3. get_growth_summary 评估干预执行情况
4. search_knowledge 搜索匹配的干预方法
5. 综合以上信息生成个性化计划

【工具使用原则】
- 尽可能在一次回复中同时调用多个工具（tool_calls 数组），减少往返次数
- 工具返回的是摘要，基于摘要做决策即可
- 只有用户明确要求"帮我创建"、"加入计划"或类似请求时，才调用 create_training_tasks
- 如果信息已经足够，直接输出干预计划，不要继续调用工具

【安全边界】
- 建议仅供家庭参考，不构成医疗诊断或治疗方案
- 不要输出任何药物相关建议
- 推荐的训练任务名称要具体、可执行

【输出格式】
最终干预计划必须是严格的 JSON 格式，包含以下字段：
{
  "planSummary": "一句话概括本次干预计划的核心目标",
  "dailySchedule": [
    { "time": "上午", "task": "训练名称", "details": "具体步骤和时长" }
  ],
  "weeklyFocus": ["本周重点关注项1", "本周重点关注项2"],
  "tools": ["推荐使用的教具或辅助工具"],
  "notes": "注意事项和温馨提示"
}
```

### 精简版（后续轮次用，~300 tokens）

```
你是星宝家庭互助平台的干预计划推荐助手。
基于已获取的工具调用结果，继续推理并决定下一步：
要么继续调用工具补充信息，要么输出符合指定 JSON Schema 的最终干预计划。
不要加入额外文字。
```

**每轮节省 ~900 tokens**（完整版 1200 → 精简版 300）。

---

## 六、Token 消耗估算

| 场景 | 输入 tokens | 输出 tokens | 小计 |
|---|---|---|---|
| 第 1 轮（完整版 + tools 定义） | ~2500 | ~200 | ~2700 |
| 第 2 轮（精简版 + history + tool 结果） | ~1200 | ~150 | ~1350 |
| 第 3 轮 | ~1500 | ~150 | ~1650 |
| 第 4 轮（最终输出 JSON） | ~1800 | ~800 | ~2600 |
| 5 轮平均 | | | **~2100/轮** |
| 一次完整执行（4-6 轮） | | | **~8000-12000 tokens** |
| 50 万免费额度可执行 | | | **~40-60 次完整执行** |

### 省钱优化清单

| 优化点 | 效果 | 已在本设计中落地 |
|---|---|---|
| System Prompt 拆版（精简版只留角色） | 每轮 -900 tokens | ✅ |
| tools 参数只首轮传 | 后续轮次 -300 tokens | ✅ |
| 工具返回值只给摘要 | 单次工具返回 -50% | ✅（摘要截断 300 字） |
| 引导 LLM 合并调用多个工具 | 减少轮数 | ✅（在完整 Prompt 里写明） |
| maxIterations = 10 兜底 | 防止死循环烧 token | ✅ |

### 豆包免费额度现状

- 每个新开通的模型独立赠送 **50 万 tokens**
- 本项目已使用 `doubao-seed-2-1-turbo-260628`，已有 50 万额度
- Embedding 模型也有独立免费额度（每次检索约 500 tokens，影响不大）
- 额度烧完后有"19 元得 38 元额度"的活动，足够再跑上千次

---

## 七、安全边界

| 边界层 | 做什么 | 实现在哪 |
|---|---|---|
| **入口校验** | childId 必须属于当前 userId | `AgentController`（复用 `ChildController` 同一套逻辑） |
| **敏感同意** | 访问孩子档案前必须有隐私同意 | `AgentController` 调用 `privacyService.requireSensitiveConsent()` |
| **工具白名单** | Agent 只能调用 5 个预注册工具 | `AgentTools.invoke()` 用 switch-case 路由 |
| **写操作保护** | `create_training_tasks` 的 description 里写明"用户明确要求才调" | `AgentTools.createTrainingTasksDef()` 返回的 JSON Schema |
| **死循环保护** | maxIterations = 10，超时兜底返回友好信息 | `AgentService` 主循环 |
| **LLM 输出约束** | System Prompt 要求输出严格 JSON Schema | `AgentService` 首轮 Prompt |

### 和项目既有安全模型的关系

- 和 `ChildController` 共享同一套 `ChildRepository + PrivacyService` 校验逻辑
- 所有涉及孩子档案的写操作都经过 `privacyService` 的敏感同意前置检查
- 与 `RiskService` 是互补关系：RiskService 做内容审核，本 Agent 做结构化决策（不涉及用户生成内容的发布）

---

## 八、API 契约

### 端点

`POST /api/v1/agent/plan`

### 请求体

```json
{
  "childId": 1,
  "userId": 100,
  "userMessage": "我家星宝 4 岁，语言发育慢，每天在家能做点什么？"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| childId | Long | ✅ | 目标孩子档案 ID |
| userId | Long | ✅ | 当前用户 ID（用于权限校验） |
| userMessage | String | ✅ | 自然语言需求描述 |

### 成功响应

```json
{
  "code": 0,
  "data": {
    "plan": "{\"planSummary\":\"...\", \"dailySchedule\":[...], ...}",
    "iterations": 4,
    "trace": [
      { "type": "think", "detail": "我需要先了解这个孩子的档案信息" },
      { "type": "tool", "detail": "get_child_profile → {nickname: \"小星\", ageYears: 4}" },
      { "type": "tool", "detail": "search_knowledge → total: 3, results: [PECS...]" },
      { "type": "final", "detail": "{\"planSummary\": \"...\"}" }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| plan | String | Agent 输出的 JSON 字符串（前端/调用方自行 parse） |
| iterations | int | 本次 ReAct 循环轮数 |
| trace | List<TraceEntry> | 执行轨迹，包含每轮的 think/tool/final |
| trace[].type | String | think（推理）/ tool（工具调用）/ final（最终答案）/ error（异常） |
| trace[].detail | String | 该步骤的摘要描述 |

---

## 九、开发分期

### Phase 1：骨架搭好，能跑 Function Calling（1-2 天）

- ✅ AgentTools：实现 ①②③ 三个读工具（档案、任务、知识搜）
- ✅ AgentService：实现基础 ReAct 循环 + 豆包 Function Calling 调用
- ✅ AgentController：端点 + 安全校验
- 🎯 验证：`curl POST /agent/plan` → 看到 Agent 先查档案、再查知识、输出最终回答

### Phase 2：补全工具 + 写操作（1 天）

- ✅ 补工具 ④ `get_growth_summary`
- ✅ 补工具 ⑤ `create_training_tasks`
- ✅ System Prompt 里写清写操作的调用条件
- 🎯 验证：用户说"帮我创建"时 Agent 真的能落库

### Phase 3：完善 System Prompt + 结构化输出（1 天）

- ✅ 完整版 + 精简版 Prompt 拆版
- ✅ 最终输出强制 JSON Schema
- ✅ 工具返回值进一步优化摘要（max 500字）
- 🎯 验证：输出可以直接 JSON.parse() 成干预计划对象

### Phase 4：安全加固 + Trace（可选，半天）

- ✅ TraceEntry 完整记录每轮 think → tool → final
- ✅ 异常场景兜底（LLM 空回复、工具超时、循环超时）
- ✅ 日志加 INFO 级别每轮迭代打印

### Phase 5：前端接入（可选，1 天）

- 在 `mobile-web` 的 chat-widget 里加"开启 Agent 模式"入口
- 前端展示 trace（调用了哪些工具、返回什么）
- 🎯 端到端可交互 Demo

---

## 十、验收 Checklist

- [ ] 后端能启动（`mvn spring-boot:run` 无报错）
- [ ] 三个新增文件编译通过
- [ ] `POST /api/v1/agent/plan` 能返回结构化 JSON 干预计划
- [ ] 有执行轨迹 trace（能看到 Agent 调用了哪些工具）
- [ ] 多轮对话有 ReAct 特征（至少 2 轮 LLM 调用）
- [ ] 不引入任何新 Maven 依赖
- [ ] 不修改任何现有文件

---

## 十一、面试准备要点（开发完成后可以讲的）

| 考点 | 面试时怎么讲 |
|---|---|
| Function Calling 原理 | "LLM 本身不会调工具，它返回 tool_calls 数组，我们自己解析、执行、塞回 messages，循环直到 LLM 输出最终答案" |
| ReAct 是什么 | "Reasoning + Acting 交替循环。每轮：LLM 思考（返回 tool_calls 或 content）→ 我们执行 → 把 observation 塞回去 → 下一轮继续" |
| tools 为什么只首轮传 | "多轮对话中重复传 tools 是浪费 token，豆包等模型在首轮见过 tools 后会记住" |
| System Prompt 为什么拆两版 | "省 token。完整版含角色+任务+格式+安全边界，后续轮次只留角色+继续推理提示" |
| 安全边界怎么做 | "三层：入口校验（childId 归属）→ 工具白名单（switch-case）→ 写操作 description 约束（LLM 只在用户明确要求时才调）" |
| 和 LangChain / Spring AI 的区别 | "框架帮你把 ReAct 循环封装了，我手写了主循环、tool 注册、消息拼装，更清楚底层发生了什么" |
| 怎么防止 Agent 死循环 | "maxIterations=10 硬保护 + 精简版 Prompt 引导 LLM 尽快输出结果" |
