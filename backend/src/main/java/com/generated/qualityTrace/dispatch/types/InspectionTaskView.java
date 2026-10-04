package com.generated.qualityTrace.dispatch.types;

import java.time.Instant;
import java.time.LocalDate;

/** 检验任务响应视图（含领取人与当时资质快照）。 */
public class InspectionTaskView {

  public Long id;
  public String taskNo;
  public String batchNo;
  public String inspectionType;
  public String standardVersion;
  public String status;
  public Long assigneeId;
  public String assigneeEmployeeNo;
  public String assigneeName;
  public Instant claimedAt;
  /** 领取时刻的资质快照字段。 */
  public Long claimedQualificationId;
  public String claimedQualificationNo;
  public LocalDate claimedQualificationValidUntil;
  public int dispatchAttempts;
  public String lastDispatchError;
  public String resultStatus;
  public String resultNote;
  public Instant submittedAt;
}
