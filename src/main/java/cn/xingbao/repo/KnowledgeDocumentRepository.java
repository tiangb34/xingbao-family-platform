package cn.xingbao.repo;

import cn.xingbao.domain.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
  List<KnowledgeDocument> findByPublishedTrueAndDeletedFalseOrderByCreatedAtDesc();
  List<KnowledgeDocument> findByCategoryAndPublishedTrueAndDeletedFalseOrderByCreatedAtDesc(String category);
}