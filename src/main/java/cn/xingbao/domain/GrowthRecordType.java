package cn.xingbao.domain;

import jakarta.persistence.Entity;

@Entity
public class GrowthRecordType extends BaseEntity {
  private String code; private String name; private boolean enabled = true; private Integer sortOrder = 100; private String remark;
  public String getCode(){return code;} public void setCode(String v){code=v;}
  public String getName(){return name;} public void setName(String v){name=v;}
  public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
  public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
  public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
}
