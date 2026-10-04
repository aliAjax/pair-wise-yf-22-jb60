package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 质量检验结论。任务提交时生成，一旦生成即长期有效——
 * 即便检验员资质后来过期/吊销，已提交结论依旧有效，追溯时以快照为准。
 *
 * <p>表中冗余了检验员姓名与资质快照（{@code qualificationId} / {@code qualificationSnapshot}），
 * 用于按批号追溯时还原「当时的检验员与当时的资质」。</p>
 */
@TableName("quality_inspection")
public class QualityInspection {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long batchId;

  /** 来源检验任务 id */
  private Long taskId;

  private Long inspectorId;

  /** 检验员姓名快照 */
  private String inspectorName;

  private String inspectionType;

  private String standardVersion;

  private String resultStatus;

  /** 提交时所使用的资质记录 id 快照 */
  private Long qualificationId;

  /** 提交时资质的文本快照，如 REQ-2026-001(首检) 有效期至 2026-12-31 */
  private String qualificationSnapshot;

  private String inspectedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public Long getInspectorId() {
    return inspectorId;
  }

  public void setInspectorId(Long inspectorId) {
    this.inspectorId = inspectorId;
  }

  public String getInspectorName() {
    return inspectorName;
  }

  public void setInspectorName(String inspectorName) {
    this.inspectorName = inspectorName;
  }

  public String getInspectionType() {
    return inspectionType;
  }

  public void setInspectionType(String inspectionType) {
    this.inspectionType = inspectionType;
  }

  public String getStandardVersion() {
    return standardVersion;
  }

  public void setStandardVersion(String standardVersion) {
    this.standardVersion = standardVersion;
  }

  public String getResultStatus() {
    return resultStatus;
  }

  public void setResultStatus(String resultStatus) {
    this.resultStatus = resultStatus;
  }

  public Long getQualificationId() {
    return qualificationId;
  }

  public void setQualificationId(Long qualificationId) {
    this.qualificationId = qualificationId;
  }

  public String getQualificationSnapshot() {
    return qualificationSnapshot;
  }

  public void setQualificationSnapshot(String qualificationSnapshot) {
    this.qualificationSnapshot = qualificationSnapshot;
  }

  public String getInspectedAt() {
    return inspectedAt;
  }

  public void setInspectedAt(String inspectedAt) {
    this.inspectedAt = inspectedAt;
  }
}
