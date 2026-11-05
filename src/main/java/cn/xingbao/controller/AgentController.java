package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse;
import cn.xingbao.repo.ChildRepository;
import cn.xingbao.service.AgentService;
import cn.xingbao.service.PrivacyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 干预计划推荐控制器
 *
 * Phase 1 端点：
 * POST /api/v1/agent/plan  —— 触发生成干预计划
 *
 * 安全边界：
 * - childId 必须属于当前 userId（和 ChildController 同一套校验）
 * - 访问孩子档案信息必须有敏感信息同意
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

  private final AgentService agentService;
  private final ChildRepository childRepo;
  private final PrivacyService privacyService;

  public AgentController(AgentService agentService,
                         ChildRepository childRepo,
                         PrivacyService privacyService) {
    this.agentService = agentService;
    this.childRepo = childRepo;
    this.privacyService = privacyService;
  }

  /**
   * 生成干预计划
   *
   * 这是整个 Agent 链路的 HTTP 入口，请求进来后依次经过：
   *   ① 权限校验（childId 是否属于该用户）—— 安全护栏第 1 层
   *   ② 隐私同意校验（访问孩子敏感信息需用户授权）
   *   ③ 交给 AgentService 跑 ReAct 循环（内部再叠加第 3、4 层护栏）
   *
   * @param request PlanRequest（含 childId、userId、userMessage）
   * @return 干预计划 + 执行轨迹 + 迭代次数
   */
  @PostMapping("/plan")
  public ApiResponse<Map<String, Object>> generatePlan(@RequestBody PlanRequest request) {
    // ── 安全校验 1：childId 必须属于当前用户 ─────────────────────
    // 不存在的档案直接报错；存在但 userId 对不上也拒绝。
    // 这样即使有人伪造请求体里的 childId，也拿不到别人的孩子数据。
    var child = childRepo.findById(request.childId())
      .orElseThrow(() -> new IllegalArgumentException("孩子档案不存在 childId=" + request.childId()));
    if (!child.getUserId().equals(request.userId())) {
      throw new IllegalArgumentException("无权访问该档案");
    }

    // ── 安全校验 2：敏感信息访问需隐私同意 ───────────────────────
    // 孩子档案、成长记录属于敏感信息，未同意则不允许 AI 读取分析。
    privacyService.requireSensitiveConsent(request.userId());

    // ── 进入 Agent 核心 ─────────────────────────────────────────
    // 把 userId 一并传下去：AgentTools 在真正执行工具前会再校验一次 childId 归属，
    // 防止 LLM 在 arguments 里"幻觉"出越权的 childId（安全护栏第 4 层）。
    var result = agentService.generatePlan(request.childId(), request.userId(), request.userMessage());

    // ── 组装响应 ────────────────────────────────────────────────
    // plan       —— LLM 输出的最终 JSON 干预计划字符串
    // iterations —— 实际跑了几轮 ReAct（便于观察 Agent 效率）
    // trace      —— 每轮的 think / tool / guard / final 记录（前端展示 + 排障）
    return ApiResponse.ok(Map.of(
      "plan", result.answer(),
      "iterations", result.iterations(),
      "trace", result.trace()
    ));
  }

  /**
   * 请求体 DTO
   */
  public record PlanRequest(Long childId, Long userId, String userMessage) {}
}
