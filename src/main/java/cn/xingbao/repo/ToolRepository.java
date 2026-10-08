package cn.xingbao.repo;
import cn.xingbao.domain.ToolResource; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ToolRepository extends JpaRepository<ToolResource,Long>{ List<ToolResource> findByPublishedTrueAndDeletedFalseOrderByCreatedAtDesc(); List<ToolResource> findByTypeAndDeletedFalse(String type); }
