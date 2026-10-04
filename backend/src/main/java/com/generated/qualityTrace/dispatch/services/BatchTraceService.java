package com.generated.qualityTrace.dispatch.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.generated.qualityTrace.dispatch.constants.DispatchLogTemplates;
import com.generated.qualityTrace.dispatch.constructors.BatchTraceViewFactory;
import com.generated.qualityTrace.dispatch.models.InspectionTask;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.repositories.InspectionTaskRepository;
import com.generated.qualityTrace.dispatch.types.BatchTraceView;
import com.generated.qualityTrace.dispatch.types.TraceInspectionView;
import com.generated.qualityTrace.dispatch.validators.DispatchValidator;

import java.util.Optional;

/**
 * 批次追溯：按批号返回全部检验任务，并为每条记录给出检验员
 * 与“当时的资质”（领取快照）以及该资质当前状态（VALID/EXPIRED/REVOKED）。
 * 已提交结论即使资质后来过期也照旧有效，追溯时仍可还原当时出结论所依据的资质。
 */
@Service
public class BatchTraceService {

  private static final Logger log = LoggerFactory.getLogger(BatchTraceService.class);

  private final InspectionTaskRepository taskRepository;
  private final InspectorQualificationService qualificationService;
  private final BatchTraceViewFactory viewFactory;
  private final DispatchValidator validator;
  private final DispatchAuditLogService auditLogService;

  public BatchTraceService(InspectionTaskRepository taskRepository,
                           InspectorQualificationService qualificationService,
                           BatchTraceViewFactory viewFactory,
                           DispatchValidator validator,
                           DispatchAuditLogService auditLogService) {
    this.taskRepository = taskRepository;
    this.qualificationService = qualificationService;
    this.viewFactory = viewFactory;
    this.validator = validator;
    this.auditLogService = auditLogService;
  }

  @Transactional(readOnly = true)
  public BatchTraceView trace(String batchNo, String actorEmployeeNo) {
    List<InspectionTask> tasks =
        taskRepository.findByBatchNoOrderByInspectionTypeAscIdAsc(batchNo);
    LocalDate today = validator.today();

    List<TraceInspectionView> views = new ArrayList<>();
    for (InspectionTask task : tasks) {
      Inspector assignee = task.getAssigneeId() == null ? null
          : qualificationService.findInspectorById(task.getAssigneeId()).orElse(null);
      // 用领取时快照的资质号查“这条资质记录的当前状态”
      InspectorQualification currentQualification =
          qualificationService.findByQualificationNo(task.getClaimedQualificationNo()).orElse(null);
      views.add(viewFactory.buildInspection(task, assignee, currentQualification, today));
    }

    BatchTraceView view = viewFactory.build(batchNo, views);
    log.info(DispatchLogTemplates.TRACE_QUERIED.formatted(batchNo));
    auditLogService.record(actorEmployeeNo, "TRACE_QUERIED", "ProductBatch", batchNo,
        "inspectionCount=" + view.inspectionCount + ",submittedCount=" + view.submittedCount);
    return view;
  }
}
