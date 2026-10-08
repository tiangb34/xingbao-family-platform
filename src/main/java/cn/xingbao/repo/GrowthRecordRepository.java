package cn.xingbao.repo;
import cn.xingbao.domain.GrowthRecord; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface GrowthRecordRepository extends JpaRepository<GrowthRecord,Long>{ List<GrowthRecord> findByChildIdAndDeletedFalseOrderByOccurredAtDesc(Long childId); }
