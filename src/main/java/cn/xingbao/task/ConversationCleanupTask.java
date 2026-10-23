package cn.xingbao.task;

import cn.xingbao.repo.ConversationMessageRepository;
import cn.xingbao.repo.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 定时清理过期对话会话
 * 每天凌晨 3 点执行，删除 7 天未活跃的会话及其消息
 */
@Component
public class ConversationCleanupTask {

  private static final Logger log = LoggerFactory.getLogger(ConversationCleanupTask.class);

  /** 会话过期天数：7 天 */
  private static final long EXPIRE_DAYS = 7;

  private final ConversationRepository conversationRepository;
  private final ConversationMessageRepository messageRepository;

  public ConversationCleanupTask(ConversationRepository conversationRepository,
                                  ConversationMessageRepository messageRepository) {
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
  }

  /**
   * 每天凌晨 3 点执行清理任务
   * cron 表达式：秒 分 时 日 月 周
   */
  @Scheduled(cron = "0 0 3 * * ?")
  public void cleanupExpiredConversations() {
    Instant expireBefore = Instant.now().minusSeconds(EXPIRE_DAYS * 24 * 3600);

    log.info("开始清理过期对话会话，过期时间: {}", expireBefore);

    // 查找所有过期会话
    var expiredConversations = conversationRepository.findAll().stream()
      .filter(c -> c.getLastActiveAt() != null && c.getLastActiveAt().isBefore(expireBefore))
      .toList();

    int deletedCount = 0;
    for (var conversation : expiredConversations) {
      // 先删除关联的消息
      messageRepository.deleteByConversationId(conversation.getId());
      // 再删除会话
      conversationRepository.delete(conversation);
      deletedCount++;
    }

    log.info("清理完成，共删除 {} 个过期会话", deletedCount);
  }
}