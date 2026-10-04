package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.repositories.AppUserMapper;
import com.generated.qualityTrace.utils.JwtUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 认证服务：校验用户名并签发 JWT。
 */
@Service
public class AuthService {

  private final AppUserMapper userMapper;
  private final String jwtSecret;
  private final long ttlMillis;

  public AuthService(AppUserMapper userMapper,
                     @Value("${app.jwt.secret}") String jwtSecret,
                     @Value("${app.jwt.ttl-ms}") long ttlMillis) {
    this.userMapper = userMapper;
    this.jwtSecret = jwtSecret;
    this.ttlMillis = ttlMillis;
  }

  /**
   * 登录并返回 token 与用户信息。用户不存在或被禁用时拒绝。
   */
  public Map<String, Object> login(String username) {
    AppUser user = userMapper.findByUsername(username);
    if (user == null) {
      throw BizException.of(ErrorCodes.AUTH_REQUIRED, "用户不存在或未登录");
    }
    if (!"ACTIVE".equals(user.getStatus())) {
      throw BizException.of(ErrorCodes.AUTH_REQUIRED, "账号已停用");
    }
    ActorRole role = ActorRole.from(user.getRole());
    String token = JwtUtils.issue(user.getId(), user.getUsername(), user.getRole(), jwtSecret, ttlMillis);
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("token", token);
    result.put("id", user.getId());
    result.put("username", user.getUsername());
    result.put("displayName", user.getDisplayName());
    result.put("role", role == null ? user.getRole() : role.name());
    return result;
  }
}
