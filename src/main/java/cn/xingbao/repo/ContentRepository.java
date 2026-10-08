package cn.xingbao.repo;
import cn.xingbao.domain.ContentItem; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ContentRepository extends JpaRepository<ContentItem,Long>{ List<ContentItem> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status); List<ContentItem> findByDeletedFalseOrderByCreatedAtDesc(); }
