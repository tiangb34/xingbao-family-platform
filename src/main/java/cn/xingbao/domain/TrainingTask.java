package cn.xingbao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
public class TrainingTask extends BaseEntity {
  private Long childId;
  private String title;
  private String category;
  @Column(precision = 10, scale = 2)
  private BigDecimal dailyTarget = BigDecimal.ONE;
  private String dailyUnit = "DAY";
  private boolean enabled = true;
  public Long getChildId(){return childId;}
  public void setChildId(Long v){childId=v;}
  public String getTitle(){return title;}
  public void setTitle(String v){title=v;}
  public String getCategory(){return category;}
  public void setCategory(String v){category=v;}
  public BigDecimal getDailyTarget(){return dailyTarget;}
  public void setDailyTarget(BigDecimal v){dailyTarget=v;}
  public String getDailyUnit(){return dailyUnit;}
  public void setDailyUnit(String v){dailyUnit=v;}
  public boolean isEnabled(){return enabled;}
  public void setEnabled(boolean v){enabled=v;}
}
