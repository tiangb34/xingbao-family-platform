package cn.xingbao.domain;

import jakarta.persistence.Entity;

/** 可由管理台维护的内容审核关键词规则。 */
@Entity
public class RiskKeywordRule extends BaseEntity {
  private String keyword;
  private String riskLevel = "LOW";
  private boolean reviewRequired = true;
  private boolean enabled = true;
  private Integer priority = 100;
  private String remark;
  public String getKeyword(){return keyword;} public void setKeyword(String v){keyword=v;}
  public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;}
  public boolean isReviewRequired(){return reviewRequired;} public void setReviewRequired(boolean v){reviewRequired=v;}
  public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
  public Integer getPriority(){return priority;} public void setPriority(Integer v){priority=v;}
  public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
}
