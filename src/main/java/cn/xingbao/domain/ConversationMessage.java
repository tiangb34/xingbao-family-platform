package cn.xingbao.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 对话消息实体
 * 存储会话中的每条消息（用户输入和 AI 回复）
 * 对应数据库表：conversation_message
 */
@Entity
@Table(name = "conversation_message")
public class ConversationMessage extends BaseEntity {

  /** 所属会话 ID */
  private Long conversationId;

  /** 消息角色：user（用户）或 assistant（AI） */
  private String role;

  /** 消息内容 */
  private String content;

  public Long getConversationId() { return conversationId; }
  public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
}