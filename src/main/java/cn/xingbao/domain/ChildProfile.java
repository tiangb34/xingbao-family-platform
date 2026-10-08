package cn.xingbao.domain;

import jakarta.persistence.Entity;
import java.time.LocalDate;

@Entity
public class ChildProfile extends BaseEntity {
  private Long userId; private String nickname; private String gender; private LocalDate birthday; private String diagnosisCiphertext; private String interventionYears;
  public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getNickname(){return nickname;} public void setNickname(String v){nickname=v;}
  public String getGender(){return gender;} public void setGender(String v){gender=v;} public LocalDate getBirthday(){return birthday;} public void setBirthday(LocalDate v){birthday=v;}
  public String getDiagnosisCiphertext(){return diagnosisCiphertext;} public void setDiagnosisCiphertext(String v){diagnosisCiphertext=v;} public String getInterventionYears(){return interventionYears;} public void setInterventionYears(String v){interventionYears=v;}
}
