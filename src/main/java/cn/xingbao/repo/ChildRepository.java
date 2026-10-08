package cn.xingbao.repo;
import cn.xingbao.domain.ChildProfile; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ChildRepository extends JpaRepository<ChildProfile,Long>{ List<ChildProfile> findByUserIdAndDeletedFalse(Long userId); }
