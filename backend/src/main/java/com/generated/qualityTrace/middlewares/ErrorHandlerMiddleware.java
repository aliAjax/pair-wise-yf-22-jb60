package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.utils.Formatters;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理中间件。controller/service 抛出的异常在此统一包装为错误体，
 * 但错误码与消息来自各层（见 constants/ErrorCodes、ErrorMessages），不在此吞掉语义。
 */
@RestControllerAdvice
public class ErrorHandlerMiddleware {

  private static final Logger log = LoggerFactory.getLogger(ErrorHandlerMiddleware.class);

  /** 业务异常：按错误码映射 HTTP 状态，retryable 透传给调用方。 */
  @ExceptionHandler(BizException.class)
  public ResponseEntity<Map<String, Object>> handleBiz(BizException ex, HttpServletRequest req) {
    HttpStatus status = mapStatus(ex.getCode());
    Map<String, Object> body = base(ex.getCode(), ex.getMessage(), req.getRequestURI());
    body.put("retryable", ex.isRetryable());
    log.warn("biz error: code={} msg={} path={}", ex.getCode(), ex.getMessage(), req.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }

  /** 参数校验异常。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .orElse("参数校验失败");
    Map<String, Object> body = base(ErrorCodes.VALIDATION_FAILED,
        Formatters.format(com.generated.qualityTrace.constants.ErrorMessages.VALIDATION_FAILED, msg),
        req.getRequestURI());
    body.put("retryable", false);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  /** 兜底异常。 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleOther(Exception ex, HttpServletRequest req) {
    Map<String, Object> body = base("INTERNAL_ERROR", ex.getMessage(), req.getRequestURI());
    body.put("retryable", false);
    log.error("unexpected error: path={}", req.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private Map<String, Object> base(String code, String message, String path) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("success", false);
    body.put("code", code);
    body.put("message", message);
    body.put("path", path);
    body.put("timestamp", Formatters.now());
    return body;
  }

  private HttpStatus mapStatus(String code) {
    if (code == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return switch (code) {
      case ErrorCodes.AUTH_REQUIRED, ErrorCodes.AUTH_TOKEN_INVALID, ErrorCodes.AUTH_TOKEN_EXPIRED ->
          HttpStatus.UNAUTHORIZED;
      case ErrorCodes.RBAC_DENIED, ErrorCodes.TASK_CLAIM_FORBIDDEN,
          ErrorCodes.TASK_SUBMIT_FORBIDDEN -> HttpStatus.FORBIDDEN;
      case ErrorCodes.TASK_NOT_FOUND, ErrorCodes.QUALIFICATION_NOT_FOUND,
          ErrorCodes.INSPECTOR_NOT_FOUND, ErrorCodes.BATCH_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case ErrorCodes.RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
      case ErrorCodes.CONCURRENT_CONFLICT, ErrorCodes.TASK_ALREADY_CLAIMED,
          ErrorCodes.TASK_ALREADY_SUBMITTED -> HttpStatus.CONFLICT;
      case ErrorCodes.VALIDATION_FAILED, ErrorCodes.TASK_NOT_DISPATCHABLE,
          ErrorCodes.TASK_NOT_CLAIMABLE -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.OK;
    };
  }
}
