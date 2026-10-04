package com.generated.qualityTrace.constants;

/**
 * 日志模板集中管理。每个实体至少 4 条，写操作必须记录日志；
 * 字段变更时同步修改模板与调用处。
 */
public final class LogTemplates {

  private LogTemplates() {
  }

  // ---- 通用动作 ----
  public static final String CREATE = "create";
  public static final String UPDATE = "update";
  public static final String STATUS = "status";
  public static final String EXPORT = "export";

  // ---- 检验任务 ----
  public static final String TASK_CREATE = "创建检验任务 taskNo={0} batchNo={1} type={2} operator={3}";
  public static final String TASK_DISPATCH = "派发检验任务 taskNo={0} type={1} poolSize={2} operator={3}";
  public static final String TASK_DISPATCH_FAILED = "派发失败(可重试) taskNo={0} type={1} attempts={2} reason={3}";
  public static final String TASK_CLAIM = "领取检验任务成功 taskNo={0} inspector={1} type={2}";
  public static final String TASK_CLAIM_REJECTED = "越权领取被拒绝 taskNo={0} inspector={1} type={2} reason={3}";
  public static final String TASK_CLAIM_RACE_LOST = "领取竞争失败(先到先得) taskNo={0} inspector={1}";
  public static final String TASK_SUBMIT = "提交检验结论 taskNo={0} inspector={1} result={2}";
  public static final String TASK_EXPIRED_REVERT = "资质过期退回待派 taskNo={0} inspector={1} type={2}";

  // ---- 检验员资质 ----
  public static final String QUAL_GRANT = "授予资质 inspector={0} type={1} validUntil={2} operator={3}";
  public static final String QUAL_REVOKE = "吊销资质 inspector={0} type={1} operator={2}";
  public static final String QUAL_EXPIRE = "资质到期 inspector={0} type={1} expiredAt={2}";
  public static final String QUAL_REVALIDATE = "资质重新确认 reverted={0} operator={1}";

  // ---- 检验结论 / 追溯 ----
  public static final String INSPECTION_SUBMIT = "登记质量检验 batchNo={0} inspector={1} type={2} result={3}";
  public static final String TRACE_QUERY = "批次追溯查询 batchNo={0} operator={1}";
}
