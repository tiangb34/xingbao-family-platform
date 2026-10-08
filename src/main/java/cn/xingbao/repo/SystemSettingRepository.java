package cn.xingbao.repo;
import cn.xingbao.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SystemSettingRepository extends JpaRepository<SystemSetting,Long>{Optional<SystemSetting> findBySettingKeyAndDeletedFalse(String settingKey);}
