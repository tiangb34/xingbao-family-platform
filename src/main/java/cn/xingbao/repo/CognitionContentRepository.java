package cn.xingbao.repo;

import cn.xingbao.domain.CognitionContent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CognitionContentRepository extends JpaRepository<CognitionContent, Long> {
  List<CognitionContent> findByDeletedFalseOrderBySortOrderAscCreatedAtDesc();
  List<CognitionContent> findByPublishedTrueAndDeletedFalseOrderBySortOrderAscCreatedAtDesc();
}
