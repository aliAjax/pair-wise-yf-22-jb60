package com.generated.qualityTrace.constants;

/**
 * 错误码集中定义。service 与 controller 分别包装异常时引用同一套码，
 * 不允许在全局异常处理里吞掉具体语义。
 */
public final class ErrorCodes {

  private ErrorCodes() {
  }

  // ---- 认证 / 授权 ----
  public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
  public static final String AUTH_TOKEN_INVALID = "AUTH_TOKEN_INVALID";
  public static final String AUTH_TOKEN_EXPIRED = "AUTH_TOKEN_EXPIRED";
  public static final String RBAC_DENIED = "RBAC_DENIED";

  // ---- 通用 ----
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
  public static final String CONCURRENT_CONFLICT = "CONCURRENT_CONFLICT";
  public static final String RATE_LIMITED = "RATE_LIMITED";

  // ---- 检验任务 ----
  public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
  public static final String TASK_NOT_DISPATCHABLE = "TASK_NOT_DISPATCHABLE";
  public static final String TASK_NOT_CLAIMABLE = "TASK_NOT_CLAIMABLE";
  /** 派发失败：当前没有持有有效资质的检验员可派，可重试。 */
  public static final String TASK_DISPATCH_FAILED = "TASK_DISPATCH_FAILED";
  /** 领取失败：任务已被其他检验员先一步领走（先到先得）。 */
  public static final String TASK_ALREADY_CLAIMED = "TASK_ALREADY_CLAIMED";
  /** 越权领取：领取人不具备该检验类型的有效资质。 */
  public static final String TASK_CLAIM_FORBIDDEN = "TASK_CLAIM_FORBIDDEN";
  public static final String TASK_SUBMIT_FORBIDDEN = "TASK_SUBMIT_FORBIDDEN";
  public static final String TASK_ALREADY_SUBMITTED = "TASK_ALREADY_SUBMITTED";

  // ---- 检验员资质 ----
  public static final String INSPECTOR_NOT_FOUND = "INSPECTOR_NOT_FOUND";
  public static final String QUALIFICATION_NOT_FOUND = "QUALIFICATION_NOT_FOUND";
  public static final String QUALIFICATION_EXPIRED = "QUALIFICATION_EXPIRED";
  public static final String QUALIFICATION_REVOKED = "QUALIFICATION_REVOKED";
  public static final String QUALIFICATION_DUPLICATED = "QUALIFICATION_DUPLICATED";
  public static final String INSPECTOR_NOT_QUALIFIED = "INSPECTOR_NOT_QUALIFIED";

  // ---- 追溯 ----
  public static final String BATCH_NOT_FOUND = "BATCH_NOT_FOUND";
}
