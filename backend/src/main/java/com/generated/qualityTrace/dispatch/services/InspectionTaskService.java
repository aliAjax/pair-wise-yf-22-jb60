package com.generated.qualityTrace.dispatch.services;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorMessages;
import com.generated.qualityTrace.dispatch.constants.DispatchLogTemplates;
import com.generated.qualityTrace.dispatch.constants.InspectionTaskStatus;
import com.generated.qualityTrace.dispatch.constructors.InspectionTaskViewFactory;
import com.generated.qualityTrace.dispatch.exceptions.DispatchBusinessException;
import com.generated.qualityTrace.dispatch.models.InspectionTask;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.repositories.InspectionTaskRepository;
import com.generated.qualityTrace.dispatch.types.CreateInspectionTaskPayload;
import com.generated.qualityTrace.dispatch.types.InspectionTaskView;
import com.generated.qualityTrace.dispatch.validators.DispatchValidator;

/**
 * 检验任务服务：把“检验任务、检验员资质、批次检验结论”接起来。
 *
 * 并发模型：领取/派发/退回/提交全部走数据库条件 UPDATE（行级原子），
 * 两名检验员同时领取同一任务时只有先到的 UPDATE 匹配到行，后到者收到 409。
 */
@Service
public class InspectionTaskService {

  private static final Logger log = LoggerFactory.getLogger(InspectionTaskService.class);

  private final InspectionTaskRepository taskRepository;
  private final InspectorQualificationService qualificationService;
  private final DispatchAuditLogService auditLogService;
  private final InspectionTaskViewFactory viewFactory;
  private final DispatchValidator validator;

  public InspectionTaskService(InspectionTaskRepository taskRepository,
                               InspectorQualificationService qualificationService,
                               DispatchAuditLogService auditLogService,
                               InspectionTaskViewFactory viewFactory,
                               DispatchValidator validator) {
    this.taskRepository = taskRepository;
    this.qualificationService = qualificationService;
    this.auditLogService = auditLogService;
    this.viewFactory = viewFactory;
    this.validator = validator;
  }

  // ---------------------------------------------------------------- 创建

  @Transactional
  public InspectionTaskView create(CreateInspectionTaskPayload payload, String actorEmployeeNo) {
    validator.requireInspectionType(payload.getInspectionType());

    InspectionTask task = new InspectionTask();
    task.setTaskNo(generateTaskNo());
    task.setBatchNo(payload.getBatchNo().trim());
    task.setInspectionType(payload.getInspectionType());
    task.setStandardVersion(payload.getStandardVersion());
    task.setStatus(InspectionTaskStatus.PENDING_DISPATCH.name());
    task = taskRepository.save(task);

    log.info(DispatchLogTemplates.TASK_CREATED.formatted(
        task.getTaskNo(), task.getBatchNo(), task.getInspectionType(), task.getStandardVersion()));
    auditLogService.record(actorEmployeeNo, "TASK_CREATED", "InspectionTask", task.getTaskNo(),
        "batchNo=" + task.getBatchNo() + ",type=" + task.getInspectionType());

    // 默认创建即按检验类型自动派发给资质有效的检验员；
    // 无人可选时任务落 DISPATCH_FAILED（在同一事务里提交保存），可稍后重试，不回滚任务创建。
    if (!Boolean.FALSE.equals(payload.getAutoDispatch())) {
      dispatchInternal(task.getTaskNo(), actorEmployeeNo, false);
    }
    return getView(task.getTaskNo());
  }

  // ---------------------------------------------------------------- 派发

  /**
   * 系统派发（HTTP 入口）：无资质有效检验员时抛 422，任务落 DISPATCH_FAILED 可重试。
   * noRollbackFor 保证“派发失败状态 + 尝试次数 + 失败原因”已落库，异常仅用于告知调用方，
   * 否则重试时任务仍是 PENDING_DISPATCH，丢失失败记录。
   */
  @Transactional(
      rollbackFor = Exception.class,
      noRollbackFor = DispatchBusinessException.NoQualifiedInspectorException.class)
  public InspectionTaskView dispatch(String taskNo, String actorEmployeeNo) {
    return dispatchInternal(taskNo, actorEmployeeNo, true);
  }

  /**
   * 系统派发内部实现：在“该检验类型资质有效”的检验员里选人，条件 UPDATE 原子占用任务。
   *
   * @param throwWhenNoCandidate true=对外派发接口，无人可选抛 422；
   *                             false=创建任务时自动派发，无人可选只落 DISPATCH_FAILED 不抛异常。
   */
  private InspectionTaskView dispatchInternal(String taskNo, String actorEmployeeNo,
                                              boolean throwWhenNoCandidate) {
    InspectionTask task = requireTask(taskNo);
    if (InspectionTaskStatus.SUBMITTED.name().equals(task.getStatus())) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_ALREADY_SUBMITTED,
          String.format(DispatchErrorMessages.TASK_ALREADY_SUBMITTED, taskNo));
    }
    if (InspectionTaskStatus.CLAIMED.name().equals(task.getStatus())) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_ALREADY_CLAIMED,
          String.format(DispatchErrorMessages.TASK_ALREADY_CLAIMED, taskNo, task.getAssigneeId()));
    }

    List<InspectorQualification> candidates =
        qualificationService.findDispatchCandidates(task.getInspectionType());
    if (candidates.isEmpty()) {
      String reason = String.format(DispatchErrorMessages.NO_QUALIFIED_INSPECTOR,
          task.getInspectionType(), taskNo);
      taskRepository.markDispatchFailed(task.getId(), reason, Instant.now());
      log.warn(DispatchLogTemplates.TASK_DISPATCH_FAILED.formatted(
          taskNo, reason, requireTask(taskNo).getDispatchAttempts()));
      auditLogService.record(actorEmployeeNo, "TASK_DISPATCH_FAILED", "InspectionTask", taskNo, reason);
      if (throwWhenNoCandidate) {
        throw DispatchBusinessException.dispatchFailed(reason);
      }
      return getView(taskNo);
    }

    // 简单确定的选人策略：候选已按到期日升序，依次尝试原子占用，
    // 第一个 UPDATE 成功者即接单人（行锁保证不会把任务派给两个人）。
    for (InspectorQualification q : candidates) {
      int updated = taskRepository.dispatch(
          task.getId(), q.getInspectorId(), q.getId(), q.getQualificationNo(),
          q.getValidUntil(), Instant.now());
      if (updated == 1) {
        Inspector inspector = qualificationService.requireActiveInspectorById(q.getInspectorId());
        log.info(DispatchLogTemplates.TASK_DISPATCHED.formatted(
            taskNo, inspector.getEmployeeNo(), q.getQualificationNo()));
        auditLogService.record(actorEmployeeNo, "TASK_DISPATCHED", "InspectionTask", taskNo,
            "inspector=" + inspector.getEmployeeNo() + ",qualificationNo=" + q.getQualificationNo());
        return getView(taskNo);
      }
    }
    // 候选都在并发窗口里被别的任务/领取动作抢占：记失败、允许重试
    String reason = String.format(DispatchErrorMessages.NO_QUALIFIED_INSPECTOR,
        task.getInspectionType(), taskNo);
    taskRepository.markDispatchFailed(task.getId(), reason, Instant.now());
    auditLogService.record(actorEmployeeNo, "TASK_DISPATCH_FAILED", "InspectionTask", taskNo, reason);
    if (throwWhenNoCandidate) {
      throw DispatchBusinessException.dispatchFailed(reason);
    }
    return getView(taskNo);
  }

  /** 派发失败重试入口：与首次派发同一路径。 */
  @Transactional
  public InspectionTaskView retryDispatch(String taskNo, String actorEmployeeNo) {
    InspectionTask task = requireTask(taskNo);
    if (!InspectionTaskStatus.DISPATCH_FAILED.name().equals(task.getStatus())
        && !InspectionTaskStatus.PENDING_DISPATCH.name().equals(task.getStatus())) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_NOT_PENDING,
          String.format(DispatchErrorMessages.TASK_NOT_PENDING, taskNo, task.getStatus()));
    }
    log.info(DispatchLogTemplates.TASK_DISPATCH_RETRY.formatted(taskNo, task.getDispatchAttempts()));
    auditLogService.record(actorEmployeeNo, "TASK_DISPATCH_RETRY", "InspectionTask", taskNo,
        "attempts=" + task.getDispatchAttempts());
    return dispatch(taskNo, actorEmployeeNo);
  }

  // ---------------------------------------------------------------- 领取

  /**
   * 检验员自行领取。两条硬规则：
   * 1) 必须持有与任务检验类型匹配且当前有效的资质，否则越权领取直接拒绝（403）；
   * 2) 原子条件 UPDATE，两名检验员并发领取同一任务时只有先到者成功，后到者 409。
   */
  @Transactional
  public InspectionTaskView claim(String taskNo, String callerEmployeeNo) {
    InspectionTask task = requireTask(taskNo);
    Inspector caller = qualificationService.requireActiveInspector(callerEmployeeNo);

    if (InspectionTaskStatus.SUBMITTED.name().equals(task.getStatus())) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_ALREADY_SUBMITTED,
          String.format(DispatchErrorMessages.TASK_ALREADY_SUBMITTED, taskNo));
    }
    if (InspectionTaskStatus.CLAIMED.name().equals(task.getStatus())) {
      if (caller.getId().equals(task.getAssigneeId())) {
        return getView(taskNo); // 本人重复领取视为幂等
      }
      Long winnerId = task.getAssigneeId();
      log.warn(DispatchLogTemplates.TASK_CLAIM_RACE_LOST.formatted(taskNo, caller.getEmployeeNo(), winnerId));
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_ALREADY_CLAIMED,
          String.format(DispatchErrorMessages.TASK_ALREADY_CLAIMED, taskNo, winnerId));
    }

    // 资质门禁：缺资质/过期/吊销 -> 403 拒绝，绝不允许接单
    InspectorQualification qualification;
    try {
      qualification = qualificationService.requireValidQualification(caller, task.getInspectionType());
    } catch (DispatchBusinessException ex) {
      log.warn(DispatchLogTemplates.TASK_CLAIM_FORBIDDEN.formatted(
          taskNo, caller.getEmployeeNo(), ex.getMessage()));
      auditLogService.record(caller.getEmployeeNo(), "TASK_CLAIM_FORBIDDEN", "InspectionTask", taskNo,
          ex.getMessage());
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.ILLEGAL_CLAIM,
          String.format(DispatchErrorMessages.ILLEGAL_CLAIM,
              caller.getEmployeeNo(), taskNo, task.getInspectionType()));
    }

    int updated = taskRepository.claimIfPending(
        task.getId(), caller.getId(), qualification.getId(), qualification.getQualificationNo(),
        qualification.getValidUntil(), Instant.now());
    if (updated == 0) {
      // 并发落败：任务已在本次校验后被先到者抢走，重读拿赢家
      InspectionTask latest = requireTask(taskNo);
      log.warn(DispatchLogTemplates.TASK_CLAIM_RACE_LOST.formatted(
          taskNo, caller.getEmployeeNo(), latest.getAssigneeId()));
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_ALREADY_CLAIMED,
          String.format(DispatchErrorMessages.TASK_ALREADY_CLAIMED, taskNo, latest.getAssigneeId()));
    }

    log.info(DispatchLogTemplates.TASK_CLAIMED.formatted(
        taskNo, caller.getEmployeeNo(), qualification.getQualificationNo(), qualification.getValidUntil()));
    auditLogService.record(caller.getEmployeeNo(), "TASK_CLAIMED", "InspectionTask", taskNo,
        "qualificationNo=" + qualification.getQualificationNo()
            + ",validUntil=" + qualification.getValidUntil());
    return getView(taskNo);
  }

  // ---------------------------------------------------------------- 提交

  /**
   * 提交检验结论：
   * - 只有领取人本人能提交（越权直接拒绝）；
   * - 提交瞬间再次校验资质仍有效；若在领取后资质过期，未提交任务退回待派，
   *   由资质复检流程重新确认接单人（本方法返回 403 提示任务已退回）；
   * - 一旦 SUBMITTED，结论照旧有效，资质之后过期不再影响该结论。
   */
  @Transactional(noRollbackFor = DispatchBusinessException.class)
  public InspectionTaskView submit(String taskNo, String resultStatusRaw, String resultNote,
                                   String callerEmployeeNo) {
    InspectionTask task = requireTask(taskNo);
    Inspector caller = qualificationService.requireActiveInspector(callerEmployeeNo);
    validator.requireResultStatus(resultStatusRaw);

    if (InspectionTaskStatus.SUBMITTED.name().equals(task.getStatus())) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_ALREADY_SUBMITTED,
          String.format(DispatchErrorMessages.TASK_ALREADY_SUBMITTED, taskNo));
    }
    if (!InspectionTaskStatus.CLAIMED.name().equals(task.getStatus()) || task.getAssigneeId() == null) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_NOT_CLAIMED,
          String.format(DispatchErrorMessages.TASK_NOT_CLAIMED, taskNo, task.getStatus()));
    }
    if (!caller.getId().equals(task.getAssigneeId())) {
      // 越权：不是自己领取的任务，直接拒绝
      log.warn(DispatchLogTemplates.TASK_CLAIM_FORBIDDEN.formatted(
          taskNo, caller.getEmployeeNo(), "非领取人提交"));
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.TASK_ASSIGNED_TO_OTHER,
          String.format(DispatchErrorMessages.TASK_ASSIGNED_TO_OTHER,
              taskNo, task.getAssigneeId(), caller.getId()));
    }

    // 提交瞬间资质必须仍有效；过期则任务退回待派，结论不产生
    Optional<InspectorQualification> valid =
        qualificationService.findValidQualification(caller.getId(), task.getInspectionType());
    if (valid.isEmpty()) {
      taskRepository.returnToPendingIfClaimed(task.getId(), caller.getId(), Instant.now());
      log.warn(DispatchLogTemplates.TASK_RETURNED.formatted(
          taskNo, caller.getEmployeeNo(), task.getClaimedQualificationNo()));
      auditLogService.record(caller.getEmployeeNo(), "TASK_RETURNED", "InspectionTask", taskNo,
          "提交时资质已过期，任务退回待派");
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.QUALIFICATION_EXPIRED,
          String.format(DispatchErrorMessages.QUALIFICATION_EXPIRED,
              caller.getEmployeeNo(), task.getInspectionType(),
              task.getClaimedQualificationNo(), task.getClaimedQualificationValidUntil()));
    }

    int updated = taskRepository.submitIfClaimedByCaller(
        task.getId(), caller.getId(), resultStatusRaw, resultNote, Instant.now());
    if (updated == 0) {
      throw DispatchBusinessException.conflict(
          DispatchErrorCodes.TASK_NOT_CLAIMED,
          String.format(DispatchErrorMessages.TASK_NOT_CLAIMED, taskNo, task.getStatus()));
    }

    log.info(DispatchLogTemplates.TASK_SUBMITTED.formatted(
        taskNo, caller.getEmployeeNo(), resultStatusRaw, task.getClaimedQualificationNo()));
    auditLogService.record(caller.getEmployeeNo(), "TASK_SUBMITTED", "InspectionTask", taskNo,
        "result=" + resultStatusRaw + ",qualificationNo=" + task.getClaimedQualificationNo());
    return getView(taskNo);
  }

  // ---------------------------------------------------------------- 查询

  @Transactional(readOnly = true)
  public InspectionTaskView getView(String taskNo) {
    InspectionTask task = requireTask(taskNo);
    Inspector assignee = task.getAssigneeId() == null ? null
        : qualificationService.findInspectorById(task.getAssigneeId()).orElse(null);
    return viewFactory.build(task, assignee);
  }

  @Transactional(readOnly = true)
  public List<InspectionTaskView> list(String status) {
    List<InspectionTask> tasks = (status == null || status.isBlank())
        ? taskRepository.findAll()
        : taskRepository.findByStatusOrderByIdAsc(status);
    Map<Long, Inspector> inspectorCache = new java.util.HashMap<>();
    return tasks.stream().map(t -> {
      Inspector assignee = null;
      if (t.getAssigneeId() != null) {
        assignee = inspectorCache.computeIfAbsent(t.getAssigneeId(),
            id -> qualificationService.findInspectorById(id).orElse(null));
      }
      return viewFactory.build(t, assignee);
    }).toList();
  }

  // ---------------------------------------------------------------- 内部

  private InspectionTask requireTask(String taskNo) {
    return taskRepository.findByTaskNo(taskNo)
        .orElseThrow(() -> DispatchBusinessException.notFound(
            DispatchErrorCodes.TASK_NOT_FOUND,
            String.format(DispatchErrorMessages.TASK_NOT_FOUND, taskNo)));
  }

  /** 任务单号：TASK-日期-序列，序列来自数据库，多实例下也不撞号。 */
  private String generateTaskNo() {
    Long seq = taskRepository.nextTaskNoSequence();
    String day = java.time.LocalDate.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
    return "TASK-" + day + "-" + String.format("%03d", seq);
  }
}
