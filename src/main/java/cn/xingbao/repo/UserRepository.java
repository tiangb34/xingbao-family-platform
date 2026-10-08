package cn.xingbao.repo;
import cn.xingbao.domain.UserAccount; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount,Long>{ Optional<UserAccount> findByPhoneAndDeletedFalse(String phone); }
