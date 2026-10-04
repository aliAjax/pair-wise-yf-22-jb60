package com.generated.qualityTrace.dispatch.constructors;

import java.time.LocalDate;
import org.springframework.stereotype.Component;
import com.generated.qualityTrace.dispatch.models.InspectionTask;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.types.InspectionTaskView;

/** 检验任务响应对象构造器：controller/service 不散写视图结构。 */
@Component
public class InspectionTaskViewFactory {

  public InspectionTaskView build(InspectionTask task, Inspector assignee) {
    InspectionTaskView v = new InspectionTaskView();
    v.id = task.getId();
    v.taskNo = task.getTaskNo();
    v.batchNo = task.getBatchNo();
    v.inspectionType = task.getInspectionType();
    v.standardVersion = task.getStandardVersion();
    v.status = task.getStatus();
    v.assigneeId = task.getAssigneeId();
    if (assignee != null) {
      v.assigneeEmployeeNo = assignee.getEmployeeNo();
      v.assigneeName = assignee.getName();
    }
    v.claimedAt = task.getClaimedAt();
    v.claimedQualificationId = task.getClaimedQualificationId();
    v.claimedQualificationNo = task.getClaimedQualificationNo();
    v.claimedQualificationValidUntil = task.getClaimedQualificationValidUntil();
    v.dispatchAttempts = task.getDispatchAttempts();
    v.lastDispatchError = task.getLastDispatchError();
    v.resultStatus = task.getResultStatus();
    v.resultNote = task.getResultNote();
    v.submittedAt = task.getSubmittedAt();
    return v;
  }

  public InspectionTaskView build(InspectionTask task) {
    return build(task, null);
  }
}
