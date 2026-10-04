package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constants.QualificationStatus;
import com.generated.qualityTrace.models.InspectorQualification;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 检验员资质响应 DTO 构造器。
 */
public final class InspectorQualificationDtoFactory {

  private InspectorQualificationDtoFactory() {
  }

  /** 构造资质响应对象，并附有效状态与剩余天数。 */
  public static Map<String, Object> create(InspectorQualification q) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", q.getId());
    dto.put("inspectorId", q.getInspectorId());
    dto.put("inspectionType", q.getInspectionType());
    dto.put("inspectionTypeLabel", typeLabel(q.getInspectionType()));
    dto.put("qualifiedFrom", q.getQualifiedFrom());
    dto.put("qualifiedUntil", q.getQualifiedUntil());
    dto.put("status", q.getStatus());
    dto.put("statusLabel", statusLabel(q.getStatus()));
    dto.put("valid", isValid(q));
    dto.put("remainingDays", remainingDays(q));
    dto.put("grantedBy", q.getGrantedBy());
    return dto;
  }

  private static boolean isValid(InspectorQualification q) {
    if (!QualificationStatus.VALID.name().equals(q.getStatus())) {
      return false;
    }
    if (q.getQualifiedUntil() == null || q.getQualifiedUntil().isBlank()) {
      return true;
    }
    return q.getQualifiedUntil().compareTo(LocalDate.now().toString()) >= 0;
  }

  private static Long remainingDays(InspectorQualification q) {
    if (q.getQualifiedUntil() == null || q.getQualifiedUntil().isBlank()) {
      return null;
    }
    try {
      long days = java.time.temporal.ChronoUnit.DAYS.between(
          LocalDate.now(), LocalDate.parse(q.getQualifiedUntil()));
      return days < 0 ? 0 : days;
    } catch (Exception e) {
      return null;
    }
  }

  private static String typeLabel(String type) {
    InspectionType t = InspectionType.from(type);
    return t == null ? type : t.label();
  }

  private static String statusLabel(String status) {
    for (QualificationStatus s : QualificationStatus.values()) {
      if (s.name().equals(status)) {
        return s.label();
      }
    }
    return status;
  }
}
