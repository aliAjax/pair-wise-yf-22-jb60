package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.utils.Formatters;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * RBAC 中间件：读取方法上的 {@link RequireRole}，校验当前登录人角色是否在允许列表内。
 * 越权直接拒绝（403 语义）。
 */
@Component
public class RbacInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }
    RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
    if (requireRole == null || requireRole.value().length == 0) {
      return true;
    }
    LoginContext.Actor actor = LoginContext.get();
    if (actor == null) {
      throw BizException.of(ErrorCodes.AUTH_REQUIRED, "缺少身份凭证，请先登录");
    }
    for (ActorRole allowed : requireRole.value()) {
      if (actor.hasRole(allowed)) {
        return true;
      }
    }
    throw BizException.of(ErrorCodes.RBAC_DENIED,
        Formatters.format(ErrorMessages.RBAC_DENIED, actor.role() == null ? "未知" : actor.role().name()));
  }
}
