package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.models.AuditLog;
import com.generated.qualityTrace.repositories.AuditLogMapper;
import com.generated.qualityTrace.utils.Formatters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 操作日志中间件：写操作（POST/PUT/PATCH/DELETE）完成后记录到 audit_log 表，
 * 包含操作人、动作、目标与时间，满足追溯与审计要求。
 */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(AuditLogInterceptor.class);

  private final AuditLogMapper auditLogMapper;

  public AuditLogInterceptor(AuditLogMapper auditLogMapper) {
    this.auditLogMapper = auditLogMapper;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    String method = request.getMethod();
    if (!"POST".equals(method) && !"PUT".equals(method)
        && !"PATCH".equals(method) && !"DELETE".equals(method)) {
      return;
    }
    // 仅记录成功的写操作（2xx）
    if (response.getStatus() < 200 || response.getStatus() >= 300) {
      return;
    }
    try {
      AuditLog entry = new AuditLog();
      LoginContext.Actor actor = LoginContext.get();
      entry.setActor(actor == null ? "anonymous"
          : actor.displayName() + "(" + actor.username() + ")");
      entry.setAction(method + " " + request.getRequestURI());
      entry.setTargetType("HTTP");
      entry.setTargetId(String.valueOf(response.getStatus()));
      entry.setCreatedAt(Formatters.now());
      auditLogMapper.insert(entry);
    } catch (Exception e) {
      // 日志失败不影响主流程
      log.warn("write audit log failed: {}", e.getMessage());
    }
  }
}
