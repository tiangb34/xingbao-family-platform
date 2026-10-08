package cn.xingbao.controller;

import cn.xingbao.common.ApiResponse;
import cn.xingbao.domain.GrowthRecordType;
import cn.xingbao.repo.GrowthRecordTypeRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class GrowthRecordTypeController {
  private final GrowthRecordTypeRepository types;
  public GrowthRecordTypeController(GrowthRecordTypeRepository types){this.types=types;}
  @GetMapping("/api/v1/record-types") public ApiResponse<List<GrowthRecordType>> enabled(){return ApiResponse.ok(types.findByEnabledTrueAndDeletedFalseOrderBySortOrderAscCreatedAtAsc());}
  @GetMapping("/api/v1/admin/record-types") public ApiResponse<List<GrowthRecordType>> all(){return ApiResponse.ok(types.findByDeletedFalseOrderBySortOrderAscCreatedAtAsc());}
  @PostMapping("/api/v1/admin/record-types") public ApiResponse<GrowthRecordType> create(@RequestBody GrowthRecordType type){validate(type,false);return ApiResponse.ok(types.save(type));}
  @PutMapping("/api/v1/admin/record-types/{id}") public ApiResponse<GrowthRecordType> update(@PathVariable Long id,@RequestBody GrowthRecordType changes){GrowthRecordType type=types.findById(id).orElseThrow(()->new IllegalArgumentException("记录类型不存在"));if(changes.getCode()!=null&&!changes.getCode().equals(type.getCode())){validate(changes,true);type.setCode(changes.getCode().trim().toUpperCase());}if(changes.getName()!=null&&!changes.getName().isBlank())type.setName(changes.getName().trim());if(changes.getSortOrder()!=null)type.setSortOrder(changes.getSortOrder());type.setRemark(changes.getRemark());return ApiResponse.ok(types.save(type));}
  @PostMapping("/api/v1/admin/record-types/{id}/enabled") public ApiResponse<GrowthRecordType> enabled(@PathVariable Long id,@RequestBody Map<String,Boolean> body){GrowthRecordType type=types.findById(id).orElseThrow(()->new IllegalArgumentException("记录类型不存在"));type.setEnabled(Boolean.TRUE.equals(body.get("enabled")));return ApiResponse.ok(types.save(type));}
  @DeleteMapping("/api/v1/admin/record-types/{id}") public ApiResponse<Void> remove(@PathVariable Long id){GrowthRecordType type=types.findById(id).orElseThrow(()->new IllegalArgumentException("记录类型不存在"));type.setDeleted(true);types.save(type);return ApiResponse.ok(null);}
  private void validate(GrowthRecordType type,boolean changing){if(type.getCode()==null||!type.getCode().trim().matches("[A-Za-z][A-Za-z0-9_]{1,39}"))throw new IllegalArgumentException("类型编码需为 2-40 位英文、数字或下划线，且以字母开头");if(type.getName()==null||type.getName().isBlank())throw new IllegalArgumentException("类型名称不能为空");String code=type.getCode().trim().toUpperCase();if(types.findByCodeAndDeletedFalse(code).isPresent()&&!changing)throw new IllegalArgumentException("类型编码已存在");type.setCode(code);type.setName(type.getName().trim());if(type.getSortOrder()==null)type.setSortOrder(100);}
}
