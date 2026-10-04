package com.generated.qualityTrace.middlewares;

import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.exceptions.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流中间件：基于内存的滑动窗口计数，按「用户 + 接口」限制写操作频率。
 * 超限直接拒绝（429 语义）。生产可替换为 Redis。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  /** 窗口大小（毫秒）。 */
  private static final long WINDOW_MS = 60_000L;
  /** 单窗口最大请求数。 */
  private static final int MAX_REQUESTS = 30;

  private final Map<String, Window> counters = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // 仅限制写操作
    String method = request.getMethod();
    if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
      return true;
    }
    String actor = LoginContext.get() == null ? "anonymous" : String.valueOf(LoginContext.get().id());
    String key = actor + ":" + request.getRequestURI();
    long now = System.currentTimeMillis();
    Window window = counters.compute(key, (k, w) -> {
      if (w == null || now - w.startAt > WINDOW_MS) {
        return new Window(now, new AtomicInteger(1));
      }
      w.count.incrementAndGet();
      return w;
    });
    if (window.count.get() > MAX_REQUESTS) {
      throw BizException.of(ErrorCodes.RATE_LIMITED, ErrorMessages.RATE_LIMITED);
    }
    return true;
  }

  private record Window(long startAt, AtomicInteger count) {
  }
}
