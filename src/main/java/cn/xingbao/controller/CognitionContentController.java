package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse;
import cn.xingbao.domain.CognitionContent;
import cn.xingbao.repo.CognitionContentRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CognitionContentController {
  private final CognitionContentRepository repo;
  public CognitionContentController(CognitionContentRepository repo) { this.repo = repo; }

  @GetMapping("/cognition-contents")
  public ApiResponse<List<CognitionContent>> published() {
    return ApiResponse.ok(repo.findByPublishedTrueAndDeletedFalseOrderBySortOrderAscCreatedAtDesc());
  }

  @GetMapping("/admin/cognition-contents")
  public ApiResponse<List<CognitionContent>> all() {
    return ApiResponse.ok(repo.findByDeletedFalseOrderBySortOrderAscCreatedAtDesc());
  }

  @PostMapping("/admin/cognition-contents")
  public ApiResponse<CognitionContent> create(@RequestBody CognitionContent input) {
    return ApiResponse.ok(repo.save(normalize(input)));
  }

  @PutMapping("/admin/cognition-contents/{id}")
  public ApiResponse<CognitionContent> update(@PathVariable Long id, @RequestBody CognitionContent input) {
    CognitionContent item = get(id);
    item.setTitle(input.getTitle());
    item.setSummary(input.getSummary());
    item.setContent(input.getContent());
    item.setSortOrder(input.getSortOrder());
    item.setPublished(input.isPublished());
    return ApiResponse.ok(repo.save(normalize(item)));
  }

  @PostMapping("/admin/cognition-contents/{id}/published")
  public ApiResponse<CognitionContent> publish(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
    CognitionContent item = get(id);
    item.setPublished(Boolean.TRUE.equals(body.get("published")));
    return ApiResponse.ok(repo.save(item));
  }

  @DeleteMapping("/admin/cognition-contents/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    CognitionContent item = get(id);
    item.setDeleted(true);
    repo.save(item);
    return ApiResponse.ok(null);
  }

  private CognitionContent get(Long id) {
    return repo.findById(id).filter(item -> !item.isDeleted()).orElseThrow(() -> new IllegalArgumentException("内容不存在"));
  }

  private CognitionContent normalize(CognitionContent item) {
    if (item.getTitle() == null || item.getTitle().isBlank()) throw new IllegalArgumentException("请填写标题");
    if (item.getSummary() == null) item.setSummary("");
    if (item.getContent() == null || item.getContent().isBlank()) throw new IllegalArgumentException("请填写内容");
    if (item.getSortOrder() == null) item.setSortOrder(100);
    return item;
  }
}
