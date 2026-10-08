package cn.xingbao.domain;

import jakarta.persistence.Entity;

@Entity
public class ContentItem extends BaseEntity {
  private String title; private String summary; private String sourceUrl; private String licenseBasis; private String status = "PENDING"; private String riskLevel; private String reviewerNote;
  public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
  public String getSourceUrl(){return sourceUrl;} public void setSourceUrl(String v){sourceUrl=v;} public String getLicenseBasis(){return licenseBasis;} public void setLicenseBasis(String v){licenseBasis=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getRiskLevel(){return riskLevel;} public void setRiskLevel(String v){riskLevel=v;} public String getReviewerNote(){return reviewerNote;} public void setReviewerNote(String v){reviewerNote=v;}
}
