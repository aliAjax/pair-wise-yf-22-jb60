package com.generated.qualityTrace.dispatch.middlewares;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes;
import com.generated.qualityTrace.dispatch.exceptions.DispatchBusinessException;

/**
 * 派发域全局异常处理：service/controller 抛出的业务异常在此统一包装，
 * 不在业务代码里吞异常。
 */
@RestControllerAdvice(basePackages = "com.generated.qualityTrace.dispatch")
public class ErrorHandlerDispatchMiddleware {

  private static final Logger log = LoggerFactory.getLogger(ErrorHandlerDispatchMiddleware.class);

  @ExceptionHandler(DispatchBusinessException.class)
  public ResponseEntity<Map<String, Object>> handleBusiness(DispatchBusinessException ex) {
    log.warn("业务异常 code={} status={} msg={}", ex.getCode(), ex.getHttpStatus(), ex.getMessage());
    return ResponseEntity.status(ex.getHttpStatus()).body(body(ex.getCode(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .orElse("入参校验失败");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(body(DispatchErrorCodes.VALIDATION_FAILED, msg));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
    log.error("派发域未预期异常", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(body("INTERNAL_ERROR", "服务内部错误: " + ex.getMessage()));
  }

  private Map<String, Object> body(String code, String message) {
    return Map.of("code", code, "message", message, "timestamp", Instant.now().toString());
  }
}
