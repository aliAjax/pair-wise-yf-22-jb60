package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ActorRole;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色限制注解：标注在 Controller 方法上，由 {@link RbacInterceptor} 校验当前登录人是否具备任一允许角色。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

  /** 允许访问的角色（满足其一即可）。 */
  ActorRole[] value();
}
