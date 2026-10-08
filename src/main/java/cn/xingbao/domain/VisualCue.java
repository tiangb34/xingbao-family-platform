package cn.xingbao.domain;
import jakarta.persistence.Entity;
@Entity public class VisualCue extends BaseEntity {private Long childId;private String title;private String color="#FFE1EB";private String imageUrl;public Long getChildId(){return childId;}public void setChildId(Long v){childId=v;}public String getTitle(){return title;}public void setTitle(String v){title=v;}public String getColor(){return color;}public void setColor(String v){color=v;}public String getImageUrl(){return imageUrl;}public void setImageUrl(String v){imageUrl=v;}}
