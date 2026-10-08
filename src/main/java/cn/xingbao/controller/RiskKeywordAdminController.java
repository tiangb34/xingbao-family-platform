package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse;
import cn.xingbao.domain.RiskKeywordRule;
import cn.xingbao.repo.RiskKeywordRuleRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/risk-keywords")
public class RiskKeywordAdminController {
  private final RiskKeywordRuleRepository rules;
  public RiskKeywordAdminController(RiskKeywordRuleRepository rules){this.rules=rules;}
  @GetMapping public ApiResponse<List<RiskKeywordRule>> list(){return ApiResponse.ok(rules.findByDeletedFalseOrderByPriorityAscCreatedAtAsc());}
  @PostMapping public ApiResponse<RiskKeywordRule> create(@RequestBody RiskKeywordRule rule){validate(rule);return ApiResponse.ok(rules.save(rule));}
  @PutMapping("/{id}") public ApiResponse<RiskKeywordRule> update(@PathVariable Long id,@RequestBody RiskKeywordRule input){RiskKeywordRule rule=rules.findById(id).orElseThrow(()->new IllegalArgumentException("规则不存在"));rule.setKeyword(input.getKeyword());rule.setRiskLevel(input.getRiskLevel());rule.setReviewRequired(input.isReviewRequired());rule.setEnabled(input.isEnabled());rule.setPriority(input.getPriority());rule.setRemark(input.getRemark());validate(rule);return ApiResponse.ok(rules.save(rule));}
  @PostMapping("/{id}/enabled") public ApiResponse<RiskKeywordRule> enabled(@PathVariable Long id,@RequestBody EnableRequest input){RiskKeywordRule rule=rules.findById(id).orElseThrow(()->new IllegalArgumentException("规则不存在"));rule.setEnabled(input.enabled());return ApiResponse.ok(rules.save(rule));}
  @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id){RiskKeywordRule rule=rules.findById(id).orElseThrow(()->new IllegalArgumentException("规则不存在"));rule.setDeleted(true);rules.save(rule);return ApiResponse.ok(null);}
  private static void validate(RiskKeywordRule r){if(r.getKeyword()==null||r.getKeyword().isBlank())throw new IllegalArgumentException("关键词不能为空");if(!List.of("HIGH","MEDIUM","LOW").contains(r.getRiskLevel()))throw new IllegalArgumentException("风险等级不合法");if(r.getPriority()==null||r.getPriority()<0)throw new IllegalArgumentException("优先级必须为非负整数");}
  public record EnableRequest(boolean enabled){}
}
