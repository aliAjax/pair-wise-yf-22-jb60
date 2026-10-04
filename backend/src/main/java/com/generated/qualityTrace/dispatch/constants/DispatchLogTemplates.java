package com.generated.qualityTrace.dispatch.constants;

/** 派发域审计日志模板（同时写入应用日志与 audit_log 表）。 */
public final class DispatchLogTemplates {
  private DispatchLogTemplates() {}

  public static final String TASK_CREATED =
      "[DISPATCH] 检验任务创建: taskNo=%s batchNo=%s type=%s standard=%s";
  public static final String TASK_DISPATCHED =
      "[DISPATCH] 任务派发成功: taskNo=%s inspector=%s qualificationNo=%s";
  public static final String TASK_DISPATCH_FAILED =
      "[DISPATCH] 任务派发失败: taskNo=%s reason=%s attempts=%d";
  public static final String TASK_DISPATCH_RETRY =
      "[DISPATCH] 任务派发重试: taskNo=%s attempts=%d";
  public static final String TASK_CLAIMED =
      "[DISPATCH] 任务领取成功(先到先得): taskNo=%s inspector=%s qualificationNo=%s validUntil=%s";
  public static final String TASK_CLAIM_RACE_LOST =
      "[DISPATCH] 并发领取落败: taskNo=%s inspector=%s winner=%s";
  public static final String TASK_CLAIM_FORBIDDEN =
      "[DISPATCH] 越权领取拒绝: taskNo=%s inspector=%s reason=%s";
  public static final String TASK_SUBMITTED =
      "[DISPATCH] 检验结论提交: taskNo=%s inspector=%s result=%s qualificationNo=%s";
  public static final String QUALIFICATION_RECHECK =
      "[DISPATCH] 资质复检完成: batchNo=%s 复检领取人=%d 退回待派=%d 已提交保持有效=%d";
  public static final String TASK_RETURNED =
      "[DISPATCH] 资质过期任务退回待派: taskNo=%s inspector=%s qualificationNo=%s";
  public static final String TRACE_QUERIED =
      "[DISPATCH] 批次追溯查询: batchNo=%s";
}
