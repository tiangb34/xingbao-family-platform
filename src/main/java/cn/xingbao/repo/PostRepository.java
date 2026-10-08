package cn.xingbao.repo;
import cn.xingbao.domain.CommunityPost; import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface PostRepository extends JpaRepository<CommunityPost,Long>{ List<CommunityPost> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status); Page<CommunityPost> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status, Pageable pageable); List<CommunityPost> findByDeletedFalseOrderByCreatedAtDesc(); }
