package com.generated.qualityTrace.constructors;

import com.generated.qualityTrace.constants.InspectionTaskStatus;
import com.generated.qualityTrace.models.InspectionTask;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 检验任务响应 DTO 构造器。页面/接口不得直接散写任务结构，统一经此构造。
 */
public final class InspectionTaskDtoFactory {

  private InspectionTaskDtoFactory() {
  }

  /** 构造任务响应对象。 */
  public static Map<String, Object> create(InspectionTask task) {
    Map<String, Object> dto = new LinkedHashMap<>();
    dto.put("id", task.getId());
    dto.put("taskNo", task.getTaskNo());
    dto.put("batchId", task.getBatchId());
    dto.put("inspectionType", task.getInspectionType());
    dto.put("status", task.getStatus());
    dto.put("statusLabel", statusLabel(task.getStatus()));
    dto.put("assigneeId", task.getAssigneeId());
    dto.put("dispatchAttempts", task.getDispatchAttempts());
    dto.put("lastDispatchError", task.getLastDispatchError());
    dto.put("dispatchedAt", task.getDispatchedAt());
    dto.put("claimedAt", task.getClaimedAt());
    dto.put("submittedAt", task.getSubmittedAt());
    return dto;
  }

  /** 带额外字段（如接单人姓名）的响应对象。 */
  public static Map<String, Object> create(InspectionTask task, Map<String, Object> extra) {
    Map<String, Object> dto = create(task);
    if (extra != null) {
      dto.putAll(extra);
    }
    return dto;
  }

  private static String statusLabel(String status) {
    for (InspectionTaskStatus s : InspectionTaskStatus.values()) {
      if (s.name().equals(status)) {
        return s.label();
      }
    }
    return status;
  }
}
