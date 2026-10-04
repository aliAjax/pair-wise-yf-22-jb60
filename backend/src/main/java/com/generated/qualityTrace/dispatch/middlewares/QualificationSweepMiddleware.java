package com.generated.qualityTrace.dispatch.middlewares;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.generated.qualityTrace.dispatch.constants.InspectionTaskStatus;
import com.generated.qualityTrace.dispatch.models.InspectionTask;
import com.generated.qualityTrace.dispatch.repositories.InspectionTaskRepository;
import com.generated.qualityTrace.dispatch.services.InspectorQualificationService;
import com.generated.qualityTrace.dispatch.services.DispatchAuditLogService;

/**
 * 资质有效期定时巡检：每天扫描 CLAIMED 任务，
 * 领取人对该检验类型已无有效资质（过期/吊销/停用）时，
 * 把“还没提交”的任务原子退回待派；SUBMITTED 不扫描，结论照旧有效。
 * 手工触发可调用 POST /api/dispatch/batches/{batchNo}/recheck-qualifications。
 */
@Component
public class QualificationSweepMiddleware {

  private static final Logger log = LoggerFactory.getLogger(QualificationSweepMiddleware.class);

  private final InspectionTaskRepository taskRepository;
  private final InspectorQualificationService qualificationService;
  private final DispatchAuditLogService auditLogService;

  public QualificationSweepMiddleware(InspectionTaskRepository taskRepository,
                                      InspectorQualificationService qualificationService,
                                      DispatchAuditLogService auditLogService) {
    this.taskRepository = taskRepository;
    this.qualificationService = qualificationService;
    this.auditLogService = auditLogService;
  }

  /** 每天 02:17 执行一次（错开整点）。 */
  @Scheduled(cron = "0 17 2 * * *")
  @Transactional
  public void sweepExpiredClaims() {
    List<InspectionTask> claimed =
        taskRepository.findByStatusOrderByIdAsc(InspectionTaskStatus.CLAIMED.name());
    int returned = 0;
    for (InspectionTask task : claimed) {
      boolean assigneeActive = task.getAssigneeId() != null
          && qualificationService.findInspectorById(task.getAssigneeId())
              .map(i -> i.isActive()).orElse(false);
      boolean stillQualified = assigneeActive
          && qualificationService
              .findValidQualification(task.getAssigneeId(), task.getInspectionType())
              .isPresent();
      if (!stillQualified && task.getAssigneeId() != null
          && taskRepository.returnToPendingIfClaimed(task.getId(), task.getAssigneeId(), Instant.now()) == 1) {
        returned++;
        log.warn("[SWEEP] 资质失效任务自动退回待派: taskNo={} inspector={} qualificationNo={}",
            task.getTaskNo(), task.getAssigneeId(), task.getClaimedQualificationNo());
        auditLogService.record("SYSTEM_SWEEP", "TASK_RETURNED", "InspectionTask", task.getTaskNo(),
            "定时巡检发现资质失效，任务退回待派");
      }
    }
    if (returned > 0) {
      log.info("[SWEEP] 资质巡检完成，退回待派任务数={}", returned);
    }
  }
}
