package cn.xingbao.repo;

import cn.xingbao.domain.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 对话消息数据访问层
 */
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

  /**
   * 查询指定会话的所有消息，按创建时间升序
   */
  List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

  /**
   * 删除指定会话的所有消息
   */
  @Modifying
  @Query("DELETE FROM ConversationMessage m WHERE m.conversationId = :conversationId")
  void deleteByConversationId(@Param("conversationId") Long conversationId);
}