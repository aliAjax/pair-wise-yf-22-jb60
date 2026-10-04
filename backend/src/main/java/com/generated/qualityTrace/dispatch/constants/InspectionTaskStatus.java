package com.generated.qualityTrace.dispatch.constants;

/**
 * 检验任务状态。
 * PENDING_DISPATCH 待派发（派发失败重试、资质过期退回都回到此态）
 * DISPATCH_FAILED 派发失败（当前无资质有效检验员），可重试
 * CLAIMED         已派给/已被检验员领取，尚未提交结论
 * SUBMITTED       已提交检验结论，结论照旧有效，不再随资质过期变化
 */
public enum InspectionTaskStatus {
  PENDING_DISPATCH,
  DISPATCH_FAILED,
  CLAIMED,
  SUBMITTED
}
