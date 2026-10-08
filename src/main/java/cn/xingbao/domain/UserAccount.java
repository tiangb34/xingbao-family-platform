package cn.xingbao.domain;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

@Entity
public class UserAccount extends BaseEntity {
  @NotBlank private String phone; private String nickname; private String status = "NORMAL"; private boolean privacyConsent; private boolean sensitiveConsent;
  public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
  public String getNickname() { return nickname; } public void setNickname(String nickname) { this.nickname = nickname; }
  public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
  public boolean isPrivacyConsent() { return privacyConsent; } public void setPrivacyConsent(boolean privacyConsent) { this.privacyConsent = privacyConsent; }
  public boolean isSensitiveConsent() { return sensitiveConsent; } public void setSensitiveConsent(boolean sensitiveConsent) { this.sensitiveConsent = sensitiveConsent; }
}
