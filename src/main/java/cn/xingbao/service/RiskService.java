package cn.xingbao.service;

import cn.xingbao.domain.RiskKeywordRule;
import cn.xingbao.repo.RiskKeywordRuleRepository;
import org.springframework.stereotype.Service;

@Service
public class RiskService {
  private final RiskKeywordRuleRepository rules;
  public RiskService(RiskKeywordRuleRepository rules){this.rules=rules;}
  public String evaluate(String text) {
    return decide(text).riskLevel();
  }
  public ReviewDecision decide(String text) {
    String normalized = text == null ? "" : text.replaceAll("\\s+", "");
    return rules.findByEnabledTrueAndDeletedFalseOrderByPriorityAscCreatedAtAsc().stream()
      .filter(rule -> normalized.contains(rule.getKeyword().replaceAll("\\s+", "")))
      .findFirst()
      .map(rule -> new ReviewDecision(rule.getRiskLevel(), rule.isReviewRequired(), rule.getKeyword()))
      .orElseGet(() -> new ReviewDecision("LOW", false, null));
  }
  public String initialStatus(String text) { return decide(text).reviewRequired() ? "PENDING" : "PUBLISHED"; }
  public record ReviewDecision(String riskLevel, boolean reviewRequired, String matchedKeyword){}
}
