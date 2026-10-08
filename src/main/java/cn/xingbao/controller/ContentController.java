package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse; import cn.xingbao.domain.ContentItem; import cn.xingbao.repo.ContentRepository; import cn.xingbao.service.RiskService; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/v1/contents") public class ContentController {
  private final ContentRepository contents; private final RiskService risk; public ContentController(ContentRepository c,RiskService r){contents=c;risk=r;}
  @GetMapping public ApiResponse<List<ContentItem>> list(){return ApiResponse.ok(contents.findByStatusAndDeletedFalseOrderByCreatedAtDesc("PUBLISHED"));}
  @PostMapping public ApiResponse<ContentItem> submit(@RequestBody ContentItem c){ if(c.getSourceUrl()==null||c.getSourceUrl().isBlank())throw new IllegalArgumentException("必须提供来源链接"); if(c.getLicenseBasis()==null||c.getLicenseBasis().isBlank())throw new IllegalArgumentException("必须说明授权或许可依据"); String text=c.getTitle()+c.getSummary(); c.setRiskLevel(risk.evaluate(text)); c.setStatus(risk.initialStatus(text)); return ApiResponse.ok(contents.save(c)); }
}
