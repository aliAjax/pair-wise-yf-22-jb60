package com.generated.qualityTrace.context;

import com.generated.qualityTrace.constants.ActorRole;

/**
 * 当前登录人上下文（基于 ThreadLocal，一次请求一个线程）。
 * 由 AuthMiddleware 校验 JWT 后写入，service/controller 据此做身份与权限判断。
 */
public final class LoginContext {

  private static final ThreadLocal<Actor> CURRENT = new ThreadLocal<>();

  private LoginContext() {
  }

  public static void set(Actor actor) {
    CURRENT.set(actor);
  }

  public static Actor get() {
    return CURRENT.get();
  }

  public static Actor require() {
    Actor actor = CURRENT.get();
    if (actor == null) {
      throw new IllegalStateException("no login actor in context");
    }
    return actor;
  }

  public static void clear() {
    CURRENT.remove();
  }

  /** 登录人快照。 */
  public record Actor(Long id, String username, String displayName, ActorRole role) {
    public boolean hasRole(ActorRole required) {
      return this.role == required;
    }

    public boolean isInspector() {
      return this.role == ActorRole.INSPECTOR;
    }
  }
}
