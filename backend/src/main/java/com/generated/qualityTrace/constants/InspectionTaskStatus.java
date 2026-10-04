package com.generated.qualityTrace.constants;

/**
 * 检验任务状态。
 *
 * <p>状态机：
 * <pre>
 *   PENDING(待派) ──dispatch──▶ POOLED(待接单) ──claim──▶ CLAIMED(已接单) ──submit──▶ SUBMITTED(已提交)
 *      ▲                            ▲                        │
 *      │                            └─── 资质过期/重新确认 ◀──┘
 *      └────────────── 派发失败重试 / 退回待派 ◀──────────────┘
 * </pre>
 *
 * <ul>
 *   <li>PENDING：任务已创建，等待派发；派发失败时也停留在此状态，可重试。</li>
 *   <li>POOLED：已派发到具备资质的检验员池中，等待领取。</li>
 *   <li>CLAIMED：已被某名检验员领取（接单人已确定）。</li>
 *   <li>SUBMITTED：检验结论已提交，流程结束；此后即便资质过期，结论依旧有效。</li>
 * </ul>
 */
public enum InspectionTaskStatus {
  PENDING("待派"),
  POOLED("待接单"),
  CLAIMED("已接单"),
  SUBMITTED("已提交");

  private final String label;

  InspectionTaskStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** 是否仍可被领取：只有待接单状态可以领取。 */
  public boolean isClaimable() {
    return this == POOLED;
  }

  /** 是否已提交结论。 */
  public boolean isSubmitted() {
    return this == SUBMITTED;
  }
}
