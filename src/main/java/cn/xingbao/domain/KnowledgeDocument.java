package cn.xingbao.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

/**
 * 知识库文档实体
 * 用于存储 AI 对话 RAG 检索的知识库内容
 * 对应数据库表：knowledge_document
 */
@Entity
public class KnowledgeDocument extends BaseEntity {

  /** 文档标题 */
  private String title;

  /** 文档分类（如：干预方法、家庭训练、康复知识等） */
  private String category;

  /** 文档正文内容（大文本字段） */
  @Lob
  private String content;

  /** 文档标签（逗号分隔，用于关键词检索） */
  private String tags;

  /** 是否已发布（未发布的文档不参与 AI 问答检索） */
  private boolean published = true;

  /** 文本向量嵌入（JSON 数组格式，用于向量相似度检索） */
  @Lob
  private String embedding;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getTags() { return tags; }
  public void setTags(String tags) { this.tags = tags; }

  public boolean isPublished() { return published; }
  public void setPublished(boolean published) { this.published = published; }

  public String getEmbedding() { return embedding; }
  public void setEmbedding(String embedding) { this.embedding = embedding; }
}