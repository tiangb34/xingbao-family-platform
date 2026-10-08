package cn.xingbao.common;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException ex) { return ResponseEntity.badRequest().body(ApiResponse.fail(400, ex.getBindingResult().getFieldError().getDefaultMessage())); }
  @ExceptionHandler(EntityNotFoundException.class)
  ResponseEntity<ApiResponse<Void>> missing(EntityNotFoundException ex) { return ResponseEntity.status(404).body(ApiResponse.fail(404, ex.getMessage())); }
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiResponse<Void>> illegal(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(ApiResponse.fail(400, ex.getMessage())); }
}
