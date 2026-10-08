package cn.xingbao.repo;
import cn.xingbao.domain.SecondHandListing; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ListingRepository extends JpaRepository<SecondHandListing,Long>{ List<SecondHandListing> findByStatusAndDeletedFalseOrderByCreatedAtDesc(String status); List<SecondHandListing> findByDeletedFalseOrderByCreatedAtDesc(); }
