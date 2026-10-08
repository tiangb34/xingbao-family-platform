package cn.xingbao.repo;

import cn.xingbao.domain.GrowthRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface GrowthRecordTypeRepository extends JpaRepository<GrowthRecordType,Long> {
  List<GrowthRecordType> findByDeletedFalseOrderBySortOrderAscCreatedAtAsc();
  List<GrowthRecordType> findByEnabledTrueAndDeletedFalseOrderBySortOrderAscCreatedAtAsc();
  Optional<GrowthRecordType> findByCodeAndDeletedFalse(String code);
}
