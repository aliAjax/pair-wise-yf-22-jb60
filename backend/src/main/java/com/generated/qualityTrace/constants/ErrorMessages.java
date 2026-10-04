package com.generated.qualityTrace.constants;

/**
 * 错误消息模板集中管理，与 {@link ErrorCodes} 一一对应。
 * 支持占位符：{0}、{1} … 由调用方按顺序填入。
 */
public final class ErrorMessages {

  private ErrorMessages() {
  }

  // ---- 认证 / 授权 ----
  public static final String AUTH_REQUIRED = "缺少身份凭证，请先登录";
  public static final String AUTH_TOKEN_INVALID = "身份凭证无效";
  public static final String AUTH_TOKEN_EXPIRED = "身份凭证已过期，请重新登录";
  public static final String RBAC_DENIED = "角色 {0} 无权执行该操作";

  // ---- 通用 ----
  public static final String VALIDATION_FAILED = "参数校验失败：{0}";
  public static final String RESOURCE_NOT_FOUND = "资源不存在：{0}";
  public static final String CONCURRENT_CONFLICT = "数据已被他人修改，请刷新后重试";
  public static final String RATE_LIMITED = "操作过于频繁，请稍后再试";

  // ---- 检验任务 ----
  public static final String TASK_NOT_FOUND = "检验任务不存在：{0}";
  public static final String TASK_NOT_DISPATCHABLE = "当前状态 {0} 不允许派发";
  public static final String TASK_NOT_CLAIMABLE = "当前状态 {0} 不允许领取";
  public static final String TASK_DISPATCH_FAILED = "派发失败：暂无持有 {0} 有效资质的检验员，可稍后重试";
  public static final String TASK_ALREADY_CLAIMED = "手慢一步，任务 {0} 已被其他检验员领取";
  public static final String TASK_CLAIM_FORBIDDEN = "越权领取：您不具备 {0} 的有效资质，任务已记录并拒绝";
  public static final String TASK_SUBMIT_FORBIDDEN = "仅接单人本人可提交任务 {0} 的结论";
  public static final String TASK_ALREADY_SUBMITTED = "任务 {0} 结论已提交，不可重复提交";

  // ---- 检验员资质 ----
  public static final String INSPECTOR_NOT_FOUND = "检验员不存在：{0}";
  public static final String QUALIFICATION_NOT_FOUND = "资质记录不存在：{0}";
  public static final String QUALIFICATION_EXPIRED = "检验员 {0} 的 {1} 资质已于 {2} 过期";
  public static final String QUALIFICATION_REVOKED = "检验员 {0} 的 {1} 资质已被吊销";
  public static final String QUALIFICATION_DUPLICATED = "检验员 {0} 的 {1} 资质已存在";
  public static final String INSPECTOR_NOT_QUALIFIED = "检验员 {0} 不具备 {1} 资质，不能接单";

  // ---- 追溯 ----
  public static final String BATCH_NOT_FOUND = "批次不存在：{0}";
}
