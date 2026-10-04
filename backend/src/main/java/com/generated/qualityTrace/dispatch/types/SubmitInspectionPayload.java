package com.generated.qualityTrace.dispatch.types;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 提交检验结论请求。 */
public class SubmitInspectionPayload {

  @NotBlank(message = "结论状态不能为空")
  @Pattern(regexp = "PASS|FAIL|CONDITIONAL_PASS|RECHECK",
      message = "结论状态只能是 PASS / FAIL / CONDITIONAL_PASS / RECHECK")
  private String resultStatus;

  private String resultNote;

  public String getResultStatus() { return resultStatus; }
  public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
  public String getResultNote() { return resultNote; }
  public void setResultNote(String resultNote) { this.resultNote = resultNote; }
}
