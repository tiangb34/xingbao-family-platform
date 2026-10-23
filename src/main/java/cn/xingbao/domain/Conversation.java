package cn.xingbao.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 对话会话实体
 * 用于管理用户的多轮对话，支持跨设备同步和历史回溯
 * 对应数据库表：conversation
 */
@Entity
@Table(name = "conversation")
public class Conversation extends BaseEntity {

  /** 会话唯一标识（前端生成或使用 UUID） */
  private String sessionId;

  /** 会话标题（取第一轮用户消息作为标题） */
  private String title;

  /** 最后活跃时间（用于过期清理） */
  private java.time.Instant lastActiveAt;

  public String getSessionId() { return sessionId; }
  public void setSessionId(String sessionId) { this.sessionId = sessionId; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public java.time.Instant getLastActiveAt() { return lastActiveAt; }
  public void setLastActiveAt(java.time.Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}