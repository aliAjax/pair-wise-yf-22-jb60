package com.generated.qualityTrace.dispatch.constructors;

import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import com.generated.qualityTrace.dispatch.models.InspectionTask;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.types.BatchTraceView;
import com.generated.qualityTrace.dispatch.types.TraceInspectionView;

/**
 * 批次追溯树构造器：把任务、检验员、领取时刻资质快照、资质当前状态组装成追溯视图。
 */
@Component
public class BatchTraceViewFactory {

  private static final DateTimeFormatter TS = DateTimeFormatter.ISO_INSTANT;

  public TraceInspectionView buildInspection(InspectionTask task, Inspector assignee,
                                             InspectorQualification currentQualification,
                                             java.time.LocalDate today) {
    TraceInspectionView v = new TraceInspectionView();
    v.taskNo = task.getTaskNo();
    v.inspectionType = task.getInspectionType();
    v.status = task.getStatus();
    v.inspectorId = task.getAssigneeId();
    if (assignee != null) {
      v.inspectorEmployeeNo = assignee.getEmployeeNo();
      v.inspectorName = assignee.getName();
    }
    v.qualificationNoAtClaim = task.getClaimedQualificationNo();
    v.qualificationValidUntilAtClaim = task.getClaimedQualificationValidUntil();
    v.resultStatus = task.getResultStatus();
    v.resultNote = task.getResultNote();
    v.claimedAt = task.getClaimedAt() == null ? null : TS.format(task.getClaimedAt());
    v.submittedAt = task.getSubmittedAt() == null ? null : TS.format(task.getSubmittedAt());

    if (task.getClaimedQualificationNo() == null) {
      v.qualificationCurrentState = "NO_SNAPSHOT";
    } else if (currentQualification == null) {
      // 快照里的资质号在当前资质表查不到（被删除），保守视为已失效
      v.qualificationCurrentState = "REVOKED";
    } else if ("REVOKED".equals(currentQualification.getStatus())) {
      v.qualificationCurrentState = "REVOKED";
    } else if (currentQualification.getValidUntil().isBefore(today)) {
      v.qualificationCurrentState = "EXPIRED";
    } else {
      v.qualificationCurrentState = "VALID";
    }
    return v;
  }

  public BatchTraceView build(String batchNo, List<TraceInspectionView> inspections) {
    BatchTraceView v = new BatchTraceView();
    v.batchNo = batchNo;
    v.inspectionCount = inspections.size();
    v.submittedCount = (int) inspections.stream().filter(i -> "SUBMITTED".equals(i.status)).count();
    v.inspections = inspections;
    return v;
  }
}
