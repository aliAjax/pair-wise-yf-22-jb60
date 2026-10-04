package com.generated.qualityTrace.dispatch.constants;

/** 派发域错误消息模板，参数按顺序填充。 */
public final class DispatchErrorMessages {
  private DispatchErrorMessages() {}

  public static final String CALLER_REQUIRED = "请求缺少 X-Inspector-Id 头，无法识别检验员身份";
  public static final String INSPECTOR_NOT_FOUND = "检验员不存在: %s";
  public static final String INSPECTOR_INACTIVE = "检验员已停用: %s";
  public static final String QUALIFICATION_MISSING = "检验员 %s 缺少检验类型 %s 的资质";
  public static final String QUALIFICATION_EXPIRED = "检验员 %s 的 %s 资质 %s 已于 %s 过期";
  public static final String QUALIFICATION_REVOKED = "检验员 %s 的资质 %s 已被吊销";
  public static final String TASK_NOT_FOUND = "检验任务不存在: %s";
  public static final String TASK_NOT_PENDING = "任务 %s 当前状态为 %s，不是待派发状态";
  public static final String TASK_ALREADY_CLAIMED = "任务 %s 已被检验员 %s 先领取";
  public static final String TASK_NOT_CLAIMED = "任务 %s 当前状态为 %s，尚未被领取，无法提交";
  public static final String TASK_ALREADY_SUBMITTED = "任务 %s 已提交结论，不可重复操作";
  public static final String TASK_ASSIGNED_TO_OTHER = "任务 %s 已派发给检验员 %s，检验员 %s 无权领取";
  public static final String NO_QUALIFIED_INSPECTOR = "检验类型 %s 当前没有资质有效且在岗的检验员，任务 %s 派发失败";
  public static final String ILLEGAL_CLAIM = "越权领取被拒绝: 检验员 %s 不具备任务 %s 要求的 %s 有效资质";
  public static final String RESULT_STATUS_INVALID = "结论状态非法: %s";
}
