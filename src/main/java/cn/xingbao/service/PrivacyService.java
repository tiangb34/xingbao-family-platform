package cn.xingbao.service;

import cn.xingbao.domain.UserAccount;
import cn.xingbao.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PrivacyService {
  private final UserRepository users;
  public PrivacyService(UserRepository users) { this.users = users; }
  public UserAccount user(Long userId) { return users.findById(userId).filter(u -> !u.isDeleted()).orElseThrow(() -> new EntityNotFoundException("用户不存在")); }
  public void requireSensitiveConsent(Long userId) { if (!user(userId).isSensitiveConsent()) throw new IllegalArgumentException("请先取得儿童敏感信息单独同意"); }
  public String maskPhone(String phone) { return phone == null || phone.length() < 7 ? "***" : phone.substring(0,3) + "****" + phone.substring(phone.length()-4); }
}
