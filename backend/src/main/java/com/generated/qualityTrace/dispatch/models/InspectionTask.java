package com.generated.qualityTrace.dispatch.models;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 检验任务。任务、检验员资质、批次检验三者的连接点：
 * - inspection_type 决定只派给持同类型有效资质的检验员；
 * - assignee_id 记录领取人，claimed_* 字段是领取时刻的资质快照；
 * - result_status/submitted_at 是批次检验结论，提交后不再受资质过期影响。
 */
@Entity
@Table(name = "inspection_task")
public class InspectionTask {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "task_no", nullable = false, unique = true)
  private String taskNo;

  @Column(name = "batch_no", nullable = false)
  private String batchNo;

  @Column(name = "inspection_type", nullable = false)
  private String inspectionType;

  @Column(name = "standard_version")
  private String standardVersion;

  @Column(name = "status", nullable = false)
  private String status = "PENDING_DISPATCH";

  @Column(name = "assignee_id")
  private Long assigneeId;

  @Column(name = "claimed_at")
  private Instant claimedAt;

  // ---- 领取时刻的资质快照：按批号追溯时返回“当时的资质” ----
  @Column(name = "claimed_qualification_id")
  private Long claimedQualificationId;

  @Column(name = "claimed_qualification_no")
  private String claimedQualificationNo;

  @Column(name = "claimed_qualification_valid_until")
  private LocalDate claimedQualificationValidUntil;

  @Column(name = "dispatch_attempts", nullable = false)
  private int dispatchAttempts = 0;

  @Column(name = "last_dispatch_error")
  private String lastDispatchError;

  @Column(name = "result_status")
  private String resultStatus;

  @Column(name = "result_note")
  private String resultNote;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTaskNo() { return taskNo; }
  public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
  public String getInspectionType() { return inspectionType; }
  public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
  public String getStandardVersion() { return standardVersion; }
  public void setStandardVersion(String standardVersion) { this.standardVersion = standardVersion; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getAssigneeId() { return assigneeId; }
  public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
  public Instant getClaimedAt() { return claimedAt; }
  public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }
  public Long getClaimedQualificationId() { return claimedQualificationId; }
  public void setClaimedQualificationId(Long claimedQualificationId) { this.claimedQualificationId = claimedQualificationId; }
  public String getClaimedQualificationNo() { return claimedQualificationNo; }
  public void setClaimedQualificationNo(String claimedQualificationNo) { this.claimedQualificationNo = claimedQualificationNo; }
  public LocalDate getClaimedQualificationValidUntil() { return claimedQualificationValidUntil; }
  public void setClaimedQualificationValidUntil(LocalDate claimedQualificationValidUntil) { this.claimedQualificationValidUntil = claimedQualificationValidUntil; }
  public int getDispatchAttempts() { return dispatchAttempts; }
  public void setDispatchAttempts(int dispatchAttempts) { this.dispatchAttempts = dispatchAttempts; }
  public String getLastDispatchError() { return lastDispatchError; }
  public void setLastDispatchError(String lastDispatchError) { this.lastDispatchError = lastDispatchError; }
  public String getResultStatus() { return resultStatus; }
  public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
  public String getResultNote() { return resultNote; }
  public void setResultNote(String resultNote) { this.resultNote = resultNote; }
  public Instant getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
