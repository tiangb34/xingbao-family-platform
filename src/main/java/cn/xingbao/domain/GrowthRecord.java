package cn.xingbao.domain;

import jakarta.persistence.Entity;
import java.time.LocalDateTime;

@Entity
public class GrowthRecord extends BaseEntity {
  private Long childId; private String type; private LocalDateTime occurredAt; private String scene; private Integer durationMinutes; private String resultNote;
  public Long getChildId(){return childId;} public void setChildId(Long v){childId=v;} public String getType(){return type;} public void setType(String v){type=v;}
  public LocalDateTime getOccurredAt(){return occurredAt;} public void setOccurredAt(LocalDateTime v){occurredAt=v;} public String getScene(){return scene;} public void setScene(String v){scene=v;}
  public Integer getDurationMinutes(){return durationMinutes;} public void setDurationMinutes(Integer v){durationMinutes=v;} public String getResultNote(){return resultNote;} public void setResultNote(String v){resultNote=v;}
}
