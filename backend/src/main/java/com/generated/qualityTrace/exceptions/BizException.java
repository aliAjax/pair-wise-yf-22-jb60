package com.generated.qualityTrace.exceptions;

/**
 * 业务异常。service 层抛出，controller/global-handler 分别包装后返回统一错误体。
 *
 * <p>{@code retryable} 用于标识「稍后重试可能成功」的失败，例如当前没有可派检验员导致的派发失败；
 * 领取竞争失败、越权领取等不属于可重试，调用方应直接拒绝或提示他人已抢先。</p>
 */
public class BizException extends RuntimeException {

  private final String code;
  private final boolean retryable;

  public BizException(String code, String message) {
    this(code, message, false);
  }

  public BizException(String code, String message, boolean retryable) {
    super(message);
    this.code = code;
    this.retryable = retryable;
  }

  public String getCode() {
    return code;
  }

  public boolean isRetryable() {
    return retryable;
  }

  /** 快速构造一个不可重试的业务异常。 */
  public static BizException of(String code, String message) {
    return new BizException(code, message, false);
  }

  /** 快速构造一个可重试的业务异常（如派发失败）。 */
  public static BizException retryable(String code, String message) {
    return new BizException(code, message, true);
  }
}
