package com.generated.qualityTrace.dispatch.constants;

/** 派发域错误码：与 http 状态在 ErrorHandlerDispatchMiddleware 中映射。 */
public final class DispatchErrorCodes {
  private DispatchErrorCodes() {}

  public static final String CALLER_REQUIRED = "CALLER_REQUIRED";
  public static final String INSPECTOR_NOT_FOUND = "INSPECTOR_NOT_FOUND";
  public static final String INSPECTOR_INACTIVE = "INSPECTOR_INACTIVE";
  public static final String QUALIFICATION_MISSING = "QUALIFICATION_MISSING";
  public static final String QUALIFICATION_EXPIRED = "QUALIFICATION_EXPIRED";
  public static final String QUALIFICATION_REVOKED = "QUALIFICATION_REVOKED";
  public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
  public static final String TASK_NOT_PENDING = "TASK_NOT_PENDING";
  public static final String TASK_ALREADY_CLAIMED = "TASK_ALREADY_CLAIMED";
  public static final String TASK_NOT_CLAIMED = "TASK_NOT_CLAIMED";
  public static final String TASK_ALREADY_SUBMITTED = "TASK_ALREADY_SUBMITTED";
  public static final String TASK_ASSIGNED_TO_OTHER = "TASK_ASSIGNED_TO_OTHER";
  public static final String NO_QUALIFIED_INSPECTOR = "NO_QUALIFIED_INSPECTOR";
  public static final String ILLEGAL_CLAIM = "ILLEGAL_CLAIM";
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
}
