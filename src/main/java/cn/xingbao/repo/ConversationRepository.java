package cn.xingbao.repo;

import cn.xingbao.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * 对话会话数据访问层
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

  /**
   * 根据 sessionId 查找会话
   */
  Optional<Conversation> findBySessionId(String sessionId);

  /**
   * 删除指定时间之前的过期会话（级联删除消息）
   */
  @Modifying
  @Query("DELETE FROM Conversation c WHERE c.lastActiveAt < :before")
  int deleteExpiredConversations(@Param("before") Instant before);
}