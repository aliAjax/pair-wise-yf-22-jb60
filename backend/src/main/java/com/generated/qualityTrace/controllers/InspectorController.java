package com.generated.qualityTrace.controllers;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.middlewares.RequireRole;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.repositories.AppUserMapper;
import com.generated.qualityTrace.routes.InspectorRoutes;
import com.generated.qualityTrace.types.CreateInspectorPayload;
import com.generated.qualityTrace.utils.Formatters;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检验员（用户）控制器：创建检验员、列出检验员。仅质量经理可创建。
 */
@RestController
public class InspectorController {

  private final AppUserMapper userMapper;

  public InspectorController(AppUserMapper userMapper) {
    this.userMapper = userMapper;
  }

  /** 创建检验员。 */
  @PostMapping(InspectorRoutes.BASE)
  @RequireRole(ActorRole.MANAGER)
  public Map<String, Object> create(@RequestBody CreateInspectorPayload payload) {
    ActorRole role = ActorRole.from(payload.role());
    if (role == null) {
      throw BizException.of(ErrorCodes.VALIDATION_FAILED, "非法角色: " + payload.role());
    }
    if (userMapper.findByUsername(payload.username()) != null) {
      throw BizException.of(ErrorCodes.VALIDATION_FAILED, "用户名已存在: " + payload.username());
    }
    AppUser user = new AppUser();
    user.setUsername(payload.username());
    user.setDisplayName(payload.displayName());
    user.setRole(role.name());
    user.setStatus("ACTIVE");
    user.setCreatedAt(Formatters.now());
    userMapper.insert(user);
    return Map.of(
        "id", user.getId(),
        "username", user.getUsername(),
        "displayName", user.getDisplayName(),
        "role", user.getRole(),
        "status", user.getStatus());
  }

  /** 列出全部检验员（质检员角色）。 */
  @GetMapping(InspectorRoutes.BASE)
  @RequireRole({ActorRole.SUPERVISOR, ActorRole.MANAGER})
  public List<Map<String, Object>> listInspectors() {
    return userMapper.selectList(new QueryWrapper<AppUser>()
            .eq("role", ActorRole.INSPECTOR.name()))
        .stream()
        .map(u -> Map.<String, Object>of(
            "id", u.getId(),
            "username", u.getUsername(),
            "displayName", u.getDisplayName(),
            "status", u.getStatus()))
        .toList();
  }
}
