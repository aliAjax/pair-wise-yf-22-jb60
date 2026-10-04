package com.generated.qualityTrace.dispatch.types;

import java.time.LocalDate;

/**
 * 追溯树中的检验节点：返回检验员以及“当时的资质”（领取时刻快照），
 * 以及该资质现在是否仍有效——即使后来过期，已提交结论照旧有效。
 */
public class TraceInspectionView {
  public String taskNo;
  public String inspectionType;
  public String status;
  public Long inspectorId;
  public String inspectorEmployeeNo;
  public String inspectorName;
  /** 领取/出结论当时的资质（快照）。 */
  public String qualificationNoAtClaim;
  public LocalDate qualificationValidUntilAtClaim;
  /** 该资质按今天的状态：EXPIRED / REVOKED / VALID / NO_SNAPSHOT。 */
  public String qualificationCurrentState;
  public String resultStatus;
  public String resultNote;
  public String claimedAt;
  public String submittedAt;
}
