package cn.xingbao.common;

import java.time.Instant;
import java.util.UUID;

public record ApiResponse<T>(int code, String message, T data, String requestId, Instant timestamp) {
  public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(200, "ok", data, UUID.randomUUID().toString(), Instant.now()); }
  public static <T> ApiResponse<T> fail(int code, String message) { return new ApiResponse<>(code, message, null, UUID.randomUUID().toString(), Instant.now()); }
}
