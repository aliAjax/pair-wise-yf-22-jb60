package com.generated.qualityTrace.dispatch.validators;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import com.generated.qualityTrace.constants.InspectionResultStatus;
import com.generated.qualityTrace.dispatch.constants.InspectionType;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorMessages;
import com.generated.qualityTrace.dispatch.exceptions.DispatchBusinessException;

/** 派发域入参/枚举校验：集中校验，不散落在 service 各处。 */
@Component
public class DispatchValidator {

  /** 校验检验类型字符串合法，返回枚举。 */
  public InspectionType requireInspectionType(String raw) {
    try {
      return InspectionType.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw DispatchBusinessException.badRequest(
          DispatchErrorCodes.VALIDATION_FAILED,
          "非法检验类型: " + raw);
    }
  }

  /** 校验结论状态合法（与共享枚举 InspectionResultStatus 对齐）。 */
  public InspectionResultStatus requireResultStatus(String raw) {
    try {
      return InspectionResultStatus.valueOf(raw);
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw DispatchBusinessException.badRequest(
          DispatchErrorCodes.VALIDATION_FAILED,
          String.format(DispatchErrorMessages.RESULT_STATUS_INVALID, raw));
    }
  }

  public LocalDate today() {
    return LocalDate.now();
  }
}
