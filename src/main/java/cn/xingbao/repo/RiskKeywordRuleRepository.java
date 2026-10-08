package cn.xingbao.repo;
import cn.xingbao.domain.RiskKeywordRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RiskKeywordRuleRepository extends JpaRepository<RiskKeywordRule,Long>{
  List<RiskKeywordRule> findByDeletedFalseOrderByPriorityAscCreatedAtAsc();
  List<RiskKeywordRule> findByEnabledTrueAndDeletedFalseOrderByPriorityAscCreatedAtAsc();
}
