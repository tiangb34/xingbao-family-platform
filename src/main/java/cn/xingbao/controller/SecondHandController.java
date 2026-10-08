package cn.xingbao.controller;
import cn.xingbao.common.ApiResponse; import cn.xingbao.domain.SecondHandListing; import cn.xingbao.repo.ListingRepository; import cn.xingbao.service.RiskService; import org.springframework.web.bind.annotation.*; import java.util.List; import java.util.Map;
@RestController @RequestMapping("/api/v1/second-hand") public class SecondHandController {
 private final ListingRepository listings; private final RiskService risk; public SecondHandController(ListingRepository l,RiskService r){listings=l;risk=r;}
 @GetMapping public ApiResponse<Map<String,Object>> list(){return ApiResponse.ok(Map.of("items",listings.findByStatusAndDeletedFalseOrderByCreatedAtDesc("PUBLISHED"),"notice","仅提供公益信息展示与线下自主沟通；平台不参与定价、支付、担保、物流或售后。"));}
 @PostMapping public ApiResponse<SecondHandListing> create(@RequestBody SecondHandListing l){String text=l.getTitle()+l.getDescription()+l.getExchangeNeed(); l.setRiskLevel(risk.evaluate(text));l.setStatus(risk.initialStatus(text));return ApiResponse.ok(listings.save(l));}
}
