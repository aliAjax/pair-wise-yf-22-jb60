package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.repositories.AppUserMapper;
import com.generated.qualityTrace.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证中间件：校验 Authorization: Bearer <token>，解析 JWT 并写入 {@link LoginContext}。
 * 放行登录接口与健康检查。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

  private final String jwtSecret;
  private final AppUserMapper userMapper;

  public AuthInterceptor(@Value("${app.jwt.secret}") String jwtSecret, AppUserMapper userMapper) {
    this.jwtSecret = jwtSecret;
    this.userMapper = userMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String path = request.getRequestURI();
    if (isPublic(path)) {
      return true;
    }
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      throw BizException.of(ErrorCodes.AUTH_REQUIRED, "缺少身份凭证，请先登录");
    }
    String token = header.substring(7).trim();
    Map<String, Object> payload;
    try {
      payload = JwtUtils.parse(token, jwtSecret);
    } catch (JwtUtils.JwtException e) {
      if (e.isExpired()) {
        throw BizException.of(ErrorCodes.AUTH_TOKEN_EXPIRED, "身份凭证已过期，请重新登录");
      }
      throw BizException.of(ErrorCodes.AUTH_TOKEN_INVALID, "身份凭证无效");
    }
    Long uid = JwtUtils.uid(payload);
    AppUser user = uid == null ? null : userMapper.selectById(uid);
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      throw BizException.of(ErrorCodes.AUTH_TOKEN_INVALID, "用户不存在或已停用");
    }
    ActorRole role = ActorRole.from(user.getRole());
    LoginContext.set(new LoginContext.Actor(user.getId(), user.getUsername(),
        user.getDisplayName(), role));
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    LoginContext.clear();
  }

  private boolean isPublic(String path) {
    return path.equals("/health")
        || path.startsWith("/api/auth/")
        || path.equals("/error");
  }
}
