package com.generated.qualityTrace.dispatch.types;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 创建检验任务请求。 */
public class CreateInspectionTaskPayload {

  @NotBlank(message = "批号不能为空")
  private String batchNo;

  @NotBlank(message = "检验类型不能为空")
  @Pattern(regexp = "FIRST_INSPECTION|PATROL_INSPECTION|FINAL_INSPECTION",
      message = "检验类型只能是 FIRST_INSPECTION / PATROL_INSPECTION / FINAL_INSPECTION")
  private String inspectionType;

  private String standardVersion;

  /** true=创建后立即按资质自动派发；默认 true，false 时进待派池等检验员领取。 */
  private Boolean autoDispatch = Boolean.TRUE;

  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
  public String getInspectionType() { return inspectionType; }
  public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
  public String getStandardVersion() { return standardVersion; }
  public void setStandardVersion(String standardVersion) { this.standardVersion = standardVersion; }
  public Boolean getAutoDispatch() { return autoDispatch; }
  public void setAutoDispatch(Boolean autoDispatch) { this.autoDispatch = autoDispatch; }
}
