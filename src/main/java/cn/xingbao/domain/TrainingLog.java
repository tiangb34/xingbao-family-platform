package cn.xingbao.domain;
import jakarta.persistence.Entity;import java.time.LocalDate;
@Entity public class TrainingLog extends BaseEntity {private Long taskId;private LocalDate recordDate;private String result;public Long getTaskId(){return taskId;}public void setTaskId(Long v){taskId=v;}public LocalDate getRecordDate(){return recordDate;}public void setRecordDate(LocalDate v){recordDate=v;}public String getResult(){return result;}public void setResult(String v){result=v;}}
