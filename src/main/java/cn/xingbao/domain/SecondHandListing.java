package cn.xingbao.domain;

import jakarta.persistence.Entity;

@Entity
public class SecondHandListing extends BaseEntity {
  private Long userId; private String title; private String description; private String exchangeNeed; private String status = "PENDING"; private String riskLevel;
  public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
  public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getExchangeNeed(){return exchangeNeed;} public void setExchangeNeed(String v){exchangeNeed=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;}
}
