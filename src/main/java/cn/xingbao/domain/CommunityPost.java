package cn.xingbao.domain;

import jakarta.persistence.Entity;

@Entity
public class CommunityPost extends BaseEntity {
  private Long userId; private String title; private String body; private String imageUrls; private boolean anonymous; private String status = "PENDING"; private String riskLevel;
  public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
  public String getBody(){return body;} public void setBody(String v){body=v;} public boolean isAnonymous(){return anonymous;} public void setAnonymous(boolean v){anonymous=v;}
  public String getImageUrls(){return imageUrls;} public void setImageUrls(String v){imageUrls=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;}
}
