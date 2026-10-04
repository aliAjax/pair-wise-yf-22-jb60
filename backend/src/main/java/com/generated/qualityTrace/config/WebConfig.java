package com.generated.qualityTrace.config;

import com.generated.qualityTrace.middlewares.AuditLogInterceptor;
import com.generated.qualityTrace.middlewares.AuthInterceptor;
import com.generated.qualityTrace.middlewares.RateLimitInterceptor;
import com.generated.qualityTrace.middlewares.RbacInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册中间件（拦截器）。执行顺序：认证 → 限流 → RBAC → 审计（afterCompletion）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final AuthInterceptor authInterceptor;
  private final RateLimitInterceptor rateLimitInterceptor;
  private final RbacInterceptor rbacInterceptor;
  private final AuditLogInterceptor auditLogInterceptor;

  public WebConfig(AuthInterceptor authInterceptor,
                   RateLimitInterceptor rateLimitInterceptor,
                   RbacInterceptor rbacInterceptor,
                   AuditLogInterceptor auditLogInterceptor) {
    this.authInterceptor = authInterceptor;
    this.rateLimitInterceptor = rateLimitInterceptor;
    this.rbacInterceptor = rbacInterceptor;
    this.auditLogInterceptor = auditLogInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor).addPathPatterns("/api/**").order(1);
    registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**").order(2);
    registry.addInterceptor(rbacInterceptor).addPathPatterns("/api/**").order(3);
    registry.addInterceptor(auditLogInterceptor).addPathPatterns("/api/**").order(4);
  }
}
