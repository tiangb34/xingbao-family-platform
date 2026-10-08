package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse; import cn.xingbao.domain.UserAccount; import cn.xingbao.repo.UserRepository;
import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
  private final UserRepository users; public AuthController(UserRepository users){this.users=users;}
  @PostMapping("/register") public ApiResponse<UserAccount> register(@Valid @RequestBody RegisterRequest r) {
    if (users.findByPhoneAndDeletedFalse(r.phone()).isPresent()) throw new IllegalArgumentException("手机号已注册");
    UserAccount u = new UserAccount(); u.setPhone(r.phone()); u.setNickname(r.nickname()); u.setPrivacyConsent(r.privacyConsent()); u.setSensitiveConsent(r.sensitiveConsent()); return ApiResponse.ok(users.save(u)); }
 @PostMapping("/login") public ApiResponse<Map<String,Object>> login(@RequestBody PhoneRequest r) { UserAccount u=users.findByPhoneAndDeletedFalse(r.phone()).orElseThrow(()->new IllegalArgumentException("用户不存在")); if(!"NORMAL".equals(u.getStatus())) throw new IllegalArgumentException("账号当前不可登录"); return ApiResponse.ok(Map.of("userId",u.getId(),"accessToken","dev-token-"+u.getId(),"expiresIn",3600)); }
  @GetMapping("/profile") public ApiResponse<UserAccount> profile(@RequestParam Long userId){return ApiResponse.ok(users.findById(userId).filter(u->!u.isDeleted()).orElseThrow(()->new IllegalArgumentException("用户不存在")));}
  public record RegisterRequest(@NotBlank String phone, @NotBlank String nickname, boolean privacyConsent, boolean sensitiveConsent){}
  public record PhoneRequest(@NotBlank String phone){}
}
