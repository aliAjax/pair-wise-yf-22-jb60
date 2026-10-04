package com.generated.qualityTrace.dispatch.exceptions;

import com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes;

/** 派发域业务异常：携带错误码与消息，由 ErrorHandlerDispatchMiddleware 映射 HTTP 状态。 */
public class DispatchBusinessException extends RuntimeException {

  private final String code;
  private final int httpStatus;

  public DispatchBusinessException(String code, String message, int httpStatus) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }

  public static DispatchBusinessException badRequest(String code, String message) {
    return new DispatchBusinessException(code, message, 400);
  }

  public static DispatchBusinessException forbidden(String code, String message) {
    return new DispatchBusinessException(code, message, 403);
  }

  public static DispatchBusinessException notFound(String code, String message) {
    return new DispatchBusinessException(code, message, 404);
  }

  public static DispatchBusinessException conflict(String code, String message) {
    return new DispatchBusinessException(code, message, 409);
  }

  /** 无资质有效检验员导致派发失败：422，任务落 DISPATCH_FAILED，允许重试。 */
  public static DispatchBusinessException dispatchFailed(String message) {
    return new NoQualifiedInspectorException(message);
  }

  /**
   * 派发失败专用异常：service 以 noRollbackFor 放行它，
   * 保证 DISPATCH_FAILED 状态、尝试次数、失败原因已经落库，只是向调用方返回错误。
   */
  public static class NoQualifiedInspectorException extends DispatchBusinessException {
    public NoQualifiedInspectorException(String message) {
      super(DispatchErrorCodes.NO_QUALIFIED_INSPECTOR, message, 422);
    }
  }

  public String getCode() { return code; }
  public int getHttpStatus() { return httpStatus; }
}
