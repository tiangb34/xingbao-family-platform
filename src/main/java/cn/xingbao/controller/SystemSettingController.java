package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse;
import cn.xingbao.domain.SystemSetting;
import cn.xingbao.repo.SystemSettingRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class SystemSettingController {
  public static final String COMMUNITY_MAX_IMAGES="COMMUNITY_MAX_IMAGES", ALLOW_COMMENT_EDIT="ALLOW_COMMENT_EDIT", ALLOW_COMMENT_DELETE="ALLOW_COMMENT_DELETE", ALLOW_POST_DELETE="ALLOW_POST_DELETE", TRAINING_CATEGORIES="TRAINING_CATEGORIES", LEARNING_CATEGORIES="LEARNING_CATEGORIES";
  private final SystemSettingRepository settings;
  public SystemSettingController(SystemSettingRepository settings){this.settings=settings;}
  @GetMapping("/api/v1/settings/community-max-images") public ApiResponse<Map<String,Integer>> maxImages(){return ApiResponse.ok(Map.of("maxImages",getMaxImages()));}
  @GetMapping("/api/v1/settings/community-permissions") public ApiResponse<Map<String,Boolean>> permissions(){return ApiResponse.ok(permissionValues());}
  @GetMapping("/api/v1/settings/training-categories") public ApiResponse<Map<String,List<String>>> trainingCategories(){return ApiResponse.ok(Map.of("categories",trainingCategoriesValue()));}
  @GetMapping("/api/v1/settings/learning-categories") public ApiResponse<Map<String,List<String>>> learningCategories(){return ApiResponse.ok(Map.of("categories",learningCategoriesValue()));}
  @GetMapping("/api/v1/admin/system-settings") public ApiResponse<Map<String,Object>> adminSettings(){Map<String,Object> result=new HashMap<>();result.put("communityMaxImages",getMaxImages());result.put("trainingCategories",trainingCategoriesValue());result.put("learningCategories",learningCategoriesValue());result.putAll(permissionValues());return ApiResponse.ok(result);}
  @PostMapping("/api/v1/admin/system-settings/community-max-images") public ApiResponse<Map<String,Integer>> update(@RequestBody Map<String,Integer> body){Integer value=body.get("maxImages");if(value==null||value<0||value>9)throw new IllegalArgumentException("图片上限必须为 0 至 9");save(COMMUNITY_MAX_IMAGES,String.valueOf(value),"社区发帖最大图片数量");return ApiResponse.ok(Map.of("communityMaxImages",value));}
  @PostMapping("/api/v1/admin/system-settings/community-permissions") public ApiResponse<Map<String,Boolean>> updatePermissions(@RequestBody Map<String,Boolean> body){if(!body.containsKey("allowCommentEdit")||!body.containsKey("allowCommentDelete")||!body.containsKey("allowPostDelete"))throw new IllegalArgumentException("请完整提交社区操作权限");save(ALLOW_COMMENT_EDIT,String.valueOf(body.get("allowCommentEdit")),"允许评论作者编辑评论");save(ALLOW_COMMENT_DELETE,String.valueOf(body.get("allowCommentDelete")),"允许评论作者删除评论");save(ALLOW_POST_DELETE,String.valueOf(body.get("allowPostDelete")),"允许帖子作者删除帖子");return ApiResponse.ok(permissionValues());}
  @PostMapping("/api/v1/admin/system-settings/training-categories") public ApiResponse<Map<String,List<String>>> updateTrainingCategories(@RequestBody Map<String,String> body){String value=body.get("categories");List<String> items=Arrays.stream(Optional.ofNullable(value).orElse("").split("[,，\\n]")).map(String::trim).filter(s->!s.isBlank()).distinct().limit(12).toList();if(items.isEmpty())throw new IllegalArgumentException("请至少保留一个训练目标分类");save(TRAINING_CATEGORIES,String.join(",",items),"每日训练目标分类");return ApiResponse.ok(Map.of("categories",items));}
  @PostMapping("/api/v1/admin/system-settings/learning-categories") public ApiResponse<Map<String,List<String>>> updateLearningCategories(@RequestBody Map<String,String> body){List<String> items=parseCategories(body.get("categories"));if(items.isEmpty())throw new IllegalArgumentException("请至少保留一个成长学习分类");save(LEARNING_CATEGORIES,String.join(",",items),"成长学习内容分类");return ApiResponse.ok(Map.of("categories",items));}
  public int getMaxImages(){try{return settings.findBySettingKeyAndDeletedFalse(COMMUNITY_MAX_IMAGES).map(SystemSetting::getSettingValue).map(Integer::parseInt).filter(v->v>=0&&v<=9).orElse(2);}catch(Exception e){return 2;}}
  public boolean allowCommentEdit(){return bool(ALLOW_COMMENT_EDIT,false);}
  public boolean allowCommentDelete(){return bool(ALLOW_COMMENT_DELETE,true);}
  public boolean allowPostDelete(){return bool(ALLOW_POST_DELETE,true);}
  public List<String> trainingCategoriesValue(){return settings.findBySettingKeyAndDeletedFalse(TRAINING_CATEGORIES).map(SystemSetting::getSettingValue).map(v->Arrays.stream(v.split(",")).map(String::trim).filter(s->!s.isBlank()).toList()).filter(v->!v.isEmpty()).orElse(List.of("沟通表达","生活自理","情绪管理","社交互动","注意力训练"));}
  public List<String> learningCategoriesValue(){return settings.findBySettingKeyAndDeletedFalse(LEARNING_CATEGORIES).map(SystemSetting::getSettingValue).map(this::parseCategories).filter(v->!v.isEmpty()).orElse(List.of("沟通表达","日常自理","情绪陪伴","家庭记录","外出准备","通用方法","家长支持"));}
  private List<String> parseCategories(String value){return Arrays.stream(Optional.ofNullable(value).orElse("").split("[,，\\n]")).map(String::trim).filter(s->!s.isBlank()).distinct().limit(20).toList();}
  private Map<String,Boolean> permissionValues(){return Map.of("allowCommentEdit",allowCommentEdit(),"allowCommentDelete",allowCommentDelete(),"allowPostDelete",allowPostDelete());}
  private boolean bool(String key,boolean fallback){return settings.findBySettingKeyAndDeletedFalse(key).map(SystemSetting::getSettingValue).map(Boolean::parseBoolean).orElse(fallback);}
  private void save(String key,String value,String remark){SystemSetting setting=settings.findBySettingKeyAndDeletedFalse(key).orElseGet(()->{SystemSetting s=new SystemSetting();s.setSettingKey(key);s.setRemark(remark);return s;});setting.setSettingValue(value);settings.save(setting);}
}
