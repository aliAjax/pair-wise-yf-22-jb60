package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * 检验任务。把「批次检验」从口头派工改为可跟踪、可并发领取、可重试的任务单。
 *
 * <p>状态见 constants/InspectionTaskStatus。并发领取通过 {@link Version} 乐观锁保证：
 * 两名检验员同时领取同一任务时，数据库条件更新只让先到的一方成功。</p>
 */
@TableName("inspection_task")
public class InspectionTask {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 任务单号，业务可读，如 TASK-20261004-0001 */
  private String taskNo;

  /** 批次 id（product_batch.id） */
  private Long batchId;

  /** 检验类型，见 constants/InspectionType */
  private String inspectionType;

  /** 状态，见 constants/InspectionTaskStatus */
  private String status;

  /** 接单人（检验员 app_user.id）；待派/待接单时为空 */
  private Long assigneeId;

  /** 派发次数（含失败重试） */
  private Integer dispatchAttempts;

  /** 最近一次派发失败原因，便于排查与重试 */
  private String lastDispatchError;

  private String dispatchedAt;

  private String claimedAt;

  private String submittedAt;

  /** 乐观锁版本号 */
  @Version
  private Integer version;

  private String createdAt;

  private String updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTaskNo() {
    return taskNo;
  }

  public void setTaskNo(String taskNo) {
    this.taskNo = taskNo;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public String getInspectionType() {
    return inspectionType;
  }

  public void setInspectionType(String inspectionType) {
    this.inspectionType = inspectionType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long assigneeId) {
    this.assigneeId = assigneeId;
  }

  public Integer getDispatchAttempts() {
    return dispatchAttempts;
  }

  public void setDispatchAttempts(Integer dispatchAttempts) {
    this.dispatchAttempts = dispatchAttempts;
  }

  public String getLastDispatchError() {
    return lastDispatchError;
  }

  public void setLastDispatchError(String lastDispatchError) {
    this.lastDispatchError = lastDispatchError;
  }

  public String getDispatchedAt() {
    return dispatchedAt;
  }

  public void setDispatchedAt(String dispatchedAt) {
    this.dispatchedAt = dispatchedAt;
  }

  public String getClaimedAt() {
    return claimedAt;
  }

  public void setClaimedAt(String claimedAt) {
    this.claimedAt = claimedAt;
  }

  public String getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(String submittedAt) {
    this.submittedAt = submittedAt;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }
}
