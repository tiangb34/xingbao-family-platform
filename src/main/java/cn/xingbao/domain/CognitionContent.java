package cn.xingbao.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

@Entity
public class CognitionContent extends BaseEntity {
  private String title;
  private String summary;
  @Lob private String content;
  private Integer sortOrder = 100;
  private boolean published = true;

  public String getTitle() { return title; }
  public void setTitle(String value) { title = value; }
  public String getSummary() { return summary; }
  public void setSummary(String value) { summary = value; }
  public String getContent() { return content; }
  public void setContent(String value) { content = value; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer value) { sortOrder = value; }
  public boolean isPublished() { return published; }
  public void setPublished(boolean value) { published = value; }
}
