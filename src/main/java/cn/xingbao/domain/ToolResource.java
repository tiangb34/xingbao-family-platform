package cn.xingbao.domain;

import jakarta.persistence.Entity;

@Entity
public class ToolResource extends BaseEntity {
  private String type; private String title; private String content; private String source; private boolean published = true;
  public String getType(){return type;} public void setType(String v){type=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getContent(){return content;} public void setContent(String v){content=v;} public String getSource(){return source;} public void setSource(String v){source=v;} public boolean isPublished(){return published;} public void setPublished(boolean v){published=v;}
}
