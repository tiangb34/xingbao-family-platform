package cn.xingbao.domain;
import jakarta.persistence.Entity;
@Entity public class CommunityReaction extends BaseEntity { private Long postId; private Long userId; private String type; public Long getPostId(){return postId;} public void setPostId(Long v){postId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;} public String getType(){return type;} public void setType(String v){type=v;} }
