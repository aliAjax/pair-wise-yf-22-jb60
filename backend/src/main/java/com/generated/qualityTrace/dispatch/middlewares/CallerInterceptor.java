package com.generated.qualityTrace.dispatch.middlewares;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证中间件（派发域）：从 X-Inspector-Id 头解析检验员工号。
 * 纯本地服务不接第三方，身份自证；RBAC 角色在 service 内按检验员档案/资质再判。
 */
@Component
public class CallerInterceptor implements HandlerInterceptor {

  public static final String INSPECTOR_HEADER = "X-Inspector-Id";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String employeeNo = request.getHeader(INSPECTOR_HEADER);
    if (employeeNo != null && !employeeNo.isBlank()) {
      CallerContext.setCallerEmployeeNo(employeeNo.trim());
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    CallerContext.clear();
  }
}
