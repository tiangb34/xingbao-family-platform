package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse; import cn.xingbao.domain.*; import cn.xingbao.repo.*; import org.springframework.web.bind.annotation.*; import java.util.*; import java.util.function.Function;

@RestController @RequestMapping("/api/v1/admin")
public class AdminController {
  private final ContentRepository contents; private final PostRepository posts; private final ListingRepository listings; private final CommentRepository comments; private final UserRepository users;
  public AdminController(ContentRepository c,PostRepository p,ListingRepository l,CommentRepository cm,UserRepository u){contents=c;posts=p;listings=l;comments=cm;users=u;}
  @GetMapping("/reviews") public ApiResponse<Map<String,Object>> reviews(@RequestParam(defaultValue="PENDING") String status){return ApiResponse.ok(Map.of("contents",filter(contents.findByDeletedFalseOrderByCreatedAtDesc(),status,ContentItem::getStatus),"posts",filter(posts.findByDeletedFalseOrderByCreatedAtDesc(),status,CommunityPost::getStatus),"comments",filter(comments.findByDeletedFalseOrderByCreatedAtDesc(),status,CommunityComment::getStatus),"secondHand",filter(listings.findByDeletedFalseOrderByCreatedAtDesc(),status,SecondHandListing::getStatus)));}
  @PostMapping("/contents/{id}/review") public ApiResponse<ContentItem> reviewContent(@PathVariable Long id,@RequestBody ReviewRequest r){ContentItem c=contents.findById(id).orElseThrow(()->new IllegalArgumentException("内容不存在"));c.setStatus(r.action());c.setReviewerNote(r.note());return ApiResponse.ok(contents.save(c));}
  @PostMapping("/posts/{id}/review") public ApiResponse<CommunityPost> reviewPost(@PathVariable Long id,@RequestBody ReviewRequest r){CommunityPost p=posts.findById(id).orElseThrow(()->new IllegalArgumentException("帖子不存在"));p.setStatus(r.action());return ApiResponse.ok(posts.save(p));}
  @PostMapping("/comments/{id}/review") public ApiResponse<CommunityComment> reviewComment(@PathVariable Long id,@RequestBody ReviewRequest r){CommunityComment c=comments.findById(id).orElseThrow(()->new IllegalArgumentException("评论不存在"));c.setStatus(r.action());return ApiResponse.ok(comments.save(c));}
  @PostMapping("/second-hand/{id}/review") public ApiResponse<SecondHandListing> reviewListing(@PathVariable Long id,@RequestBody ReviewRequest r){SecondHandListing l=listings.findById(id).orElseThrow(()->new IllegalArgumentException("信息不存在"));l.setStatus(r.action());return ApiResponse.ok(listings.save(l));}
  @PostMapping("/users/{id}/status") public ApiResponse<UserAccount> status(@PathVariable Long id,@RequestBody StatusRequest r){UserAccount u=users.findById(id).orElseThrow(()->new IllegalArgumentException("用户不存在"));if(!List.of("NORMAL","FROZEN","BLACKLIST").contains(r.status()))throw new IllegalArgumentException("账号状态不合法");u.setStatus(r.status());return ApiResponse.ok(users.save(u));}
  @GetMapping("/stats") public ApiResponse<Map<String,Object>> stats(){return ApiResponse.ok(Map.of("users",users.count(),"contents",contents.count(),"posts",posts.count(),"comments",comments.count(),"secondHandListings",listings.count()));}
  public record ReviewRequest(String action,String note) { public ReviewRequest { if(!List.of("PUBLISHED","REJECTED","OFFLINE").contains(action)) throw new IllegalArgumentException("审核动作不合法"); } }
  public record StatusRequest(String status){}
  private static <T> List<T> filter(List<T> items,String status,Function<T,String> getter){return "ALL".equals(status)?items:items.stream().filter(x->status.equals(getter.apply(x))).toList();}
}
