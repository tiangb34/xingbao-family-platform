package cn.xingbao.domain;

import jakarta.persistence.Entity;

@Entity
public class SystemSetting extends BaseEntity {
  private String settingKey; private String settingValue; private String remark;
  public String getSettingKey(){return settingKey;} public void setSettingKey(String v){settingKey=v;}
  public String getSettingValue(){return settingValue;} public void setSettingValue(String v){settingValue=v;}
  public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
}
