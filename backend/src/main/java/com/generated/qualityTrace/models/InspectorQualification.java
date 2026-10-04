package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * 检验员资质。同一检验员同一检验类型最多一条有效记录。
 *
 * <p>资质有效性 = 状态为 VALID 且（{@code qualifiedUntil} 为空表示长期，或今天 ≤ 截止日期）。
 * 派发、领取、提交三个环节都会校验资质；过期后已接单未提交的任务由批处理退回待派。</p>
 */
@TableName("inspector_qualification")
public class InspectorQualification {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 检验员（app_user.id） */
  private Long inspectorId;

  /** 检验类型，见 constants/InspectionType */
  private String inspectionType;

  /** 资质生效日期 yyyy-MM-dd */
  private String qualifiedFrom;

  /** 资质截止日期 yyyy-MM-dd；为空表示长期有效 */
  private String qualifiedUntil;

  /** 状态，见 constants/QualificationStatus：VALID / EXPIRED / REVOKED */
  private String status;

  /** 乐观锁版本号，并发授权/吊销时保证只有一方成功 */
  @Version
  private Integer version;

  private String grantedBy;

  private String createdAt;

  private String updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getInspectorId() {
    return inspectorId;
  }

  public void setInspectorId(Long inspectorId) {
    this.inspectorId = inspectorId;
  }

  public String getInspectionType() {
    return inspectionType;
  }

  public void setInspectionType(String inspectionType) {
    this.inspectionType = inspectionType;
  }

  public String getQualifiedFrom() {
    return qualifiedFrom;
  }

  public void setQualifiedFrom(String qualifiedFrom) {
    this.qualifiedFrom = qualifiedFrom;
  }

  public String getQualifiedUntil() {
    return qualifiedUntil;
  }

  public void setQualifiedUntil(String qualifiedUntil) {
    this.qualifiedUntil = qualifiedUntil;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getGrantedBy() {
    return grantedBy;
  }

  public void setGrantedBy(String grantedBy) {
    this.grantedBy = grantedBy;
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
