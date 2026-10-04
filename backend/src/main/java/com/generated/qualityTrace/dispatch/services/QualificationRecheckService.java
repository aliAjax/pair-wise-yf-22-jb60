package com.generated.qualityTrace.dispatch.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.generated.qualityTrace.dispatch.constants.DispatchLogTemplates;
import com.generated.qualityTrace.dispatch.constants.InspectionTaskStatus;
import com.generated.qualityTrace.dispatch.models.InspectionTask;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.repositories.InspectionTaskRepository;
import com.generated.qualityTrace.dispatch.types.QualificationRecheckView;
import java.util.Optional;

/**
 * 资质复检：资质过期/吊销后，对一个批号下的在途任务重新确认接单人。
 *
 * - CLAIMED 且领取人对该检验类型仍有有效资质 -> 接单人保持不变；
 * - CLAIMED 但资质已失效 -> 还没提交的任务退回待派（清空领取人与资质快照），等待重新派发/领取；
 * - SUBMITTED -> 已提交结论照旧有效，只计数不动数据。
 */
@Service
public class QualificationRecheckService {

  private static final Logger log = LoggerFactory.getLogger(QualificationRecheckService.class);

  private final InspectionTaskRepository taskRepository;
  private final InspectorQualificationService qualificationService;
  private final DispatchAuditLogService auditLogService;

  public QualificationRecheckService(InspectionTaskRepository taskRepository,
                                     InspectorQualificationService qualificationService,
                                     DispatchAuditLogService auditLogService) {
    this.taskRepository = taskRepository;
    this.qualificationService = qualificationService;
    this.auditLogService = auditLogService;
  }

  @Transactional
  public QualificationRecheckView recheck(String batchNo, String actorEmployeeNo) {
    List<InspectionTask> tasks = taskRepository.findByBatchNoOrderByInspectionTypeAscIdAsc(batchNo);

    int claimedRechecked = 0;
    int returnedToPending = 0;
    int submittedKeptValid = 0;
    List<String> confirmedTaskNos = new ArrayList<>();
    List<String> returnedTaskNos = new ArrayList<>();
    List<String> submittedTaskNos = new ArrayList<>();

    for (InspectionTask task : tasks) {
      if (InspectionTaskStatus.SUBMITTED.name().equals(task.getStatus())) {
        submittedKeptValid++;
        submittedTaskNos.add(task.getTaskNo());
        continue;
      }
      if (!InspectionTaskStatus.CLAIMED.name().equals(task.getStatus()) || task.getAssigneeId() == null) {
        continue; // PENDING_DISPATCH / DISPATCH_FAILED 本来就没有接单人
      }
      claimedRechecked++;
      Optional<Inspector> assignee = qualificationService.findInspectorById(task.getAssigneeId());
      boolean stillQualified = assignee.filter(Inspector::isActive).isPresent()
          && qualificationService
              .findValidQualification(task.getAssigneeId(), task.getInspectionType())
              .isPresent();

      if (stillQualified) {
        confirmedTaskNos.add(task.getTaskNo());
      } else {
        int updated = taskRepository.returnToPendingIfClaimed(
            task.getId(), task.getAssigneeId(), Instant.now());
        if (updated == 1) {
          returnedToPending++;
          returnedTaskNos.add(task.getTaskNo());
          log.warn(DispatchLogTemplates.TASK_RETURNED.formatted(
              task.getTaskNo(), task.getAssigneeId(), task.getClaimedQualificationNo()));
          auditLogService.record(actorEmployeeNo, "TASK_RETURNED", "InspectionTask", task.getTaskNo(),
              "资质复检不通过，任务退回待派");
        }
      }
    }

    log.info(DispatchLogTemplates.QUALIFICATION_RECHECK.formatted(
        batchNo, claimedRechecked, returnedToPending, submittedKeptValid));
    auditLogService.record(actorEmployeeNo, "QUALIFICATION_RECHECK", "ProductBatch", batchNo,
        "rechecked=" + claimedRechecked + ",returned=" + returnedToPending
            + ",submittedKept=" + submittedKeptValid);

    QualificationRecheckView view = new QualificationRecheckView();
    view.batchNo = batchNo;
    view.claimedRechecked = claimedRechecked;
    view.returnedToPending = returnedToPending;
    view.submittedKeptValid = submittedKeptValid;
    view.confirmedTaskNos = confirmedTaskNos;
    view.returnedTaskNos = returnedTaskNos;
    view.submittedTaskNos = submittedTaskNos;
    return view;
  }
}
