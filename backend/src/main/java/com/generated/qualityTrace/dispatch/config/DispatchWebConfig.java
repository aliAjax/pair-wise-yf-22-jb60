package com.generated.qualityTrace.dispatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.generated.qualityTrace.dispatch.middlewares.CallerInterceptor;

/** 注册派发域认证中间件。 */
@Configuration
public class DispatchWebConfig implements WebMvcConfigurer {

  private final CallerInterceptor callerInterceptor;

  public DispatchWebConfig(CallerInterceptor callerInterceptor) {
    this.callerInterceptor = callerInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(callerInterceptor)
        .addPathPatterns("/api/dispatch/**", "/api/trace/**");
  }
}
