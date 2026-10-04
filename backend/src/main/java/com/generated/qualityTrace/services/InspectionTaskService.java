package com.generated.qualityTrace.services;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.InspectionTaskStatus;
import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constants.QualificationStatus;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.models.InspectionTask;
import com.generated.qualityTrace.models.InspectorQualification;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.repositories.AppUserMapper;
import com.generated.qualityTrace.repositories.InspectionTaskMapper;
import com.generated.qualityTrace.repositories.InspectorQualificationMapper;
import com.generated.qualityTrace.repositories.ProductBatchMapper;
import com.generated.qualityTrace.repositories.QualityInspectionMapper;
import com.generated.qualityTrace.utils.Formatters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检验任务服务。把「任务 → 派发给有资质的检验员 → 先到先得领取 → 提交结论」串起来，
 * 并在资质过期时把已接单未提交的任务退回待派（已提交结论照旧有效）。
 */
@Service
public class InspectionTaskService {

  private static final Logger log = LoggerFactory.getLogger(InspectionTaskService.class);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final InspectionTaskMapper taskMapper;
  private final InspectorQualificationMapper qualificationMapper;
  private final AppUserMapper userMapper;
  private final ProductBatchMapper batchMapper;
  private final QualityInspectionMapper inspectionMapper;
  private final QualificationService qualificationService;

  public InspectionTaskService(InspectionTaskMapper taskMapper,
                                InspectorQualificationMapper qualificationMapper,
                                AppUserMapper userMapper,
                                ProductBatchMapper batchMapper,
                                QualityInspectionMapper inspectionMapper,
                                QualificationService qualificationService) {
    this.taskMapper = taskMapper;
    this.qualificationMapper = qualificationMapper;
    this.userMapper = userMapper;
    this.batchMapper = batchMapper;
    this.inspectionMapper = inspectionMapper;
    this.qualificationService = qualificationService;
  }

  /**
   * 创建检验任务。任务初始为待派(PENDING)，随后由主管派发。
   */
  @Transactional
  public InspectionTask createTask(Long batchId, InspectionType type, String operator) {
    ProductBatch batch = batchMapper.selectById(batchId);
    if (batch == null) {
      throw BizException.of(ErrorCodes.BATCH_NOT_FOUND,
          Formatters.format(ErrorMessages.BATCH_NOT_FOUND, String.valueOf(batchId)));
    }
    InspectionTask task = new InspectionTask();
    task.setTaskNo(generateTaskNo());
    task.setBatchId(batchId);
    task.setInspectionType(type.name());
    task.setStatus(InspectionTaskStatus.PENDING.name());
    task.setDispatchAttempts(0);
    task.setCreatedAt(Formatters.now());
    task.setUpdatedAt(Formatters.now());
    taskMapper.insert(task);
    log.info(Formatters.audit(LogTemplates.TASK_CREATE, task.getTaskNo(), batch.getBatchNo(),
        type.name(), operator));
    return task;
  }

  /**
   * 派发任务：把任务放到持有有效资质的检验员池中。
   * 若当前没有可派检验员，抛出可重试异常（任务停留在 PENDING，调用方可重试）。
   *
   * <p>派发失败属于可重试的业务结果而非系统错误，因此不回滚：派发次数与失败原因会落库，
   * 任务保持 PENDING，调用方可再次重试。</p>
   */
  @Transactional(noRollbackFor = BizException.class)
  public InspectionTask dispatch(Long taskId, String operator) {
    InspectionTask task = mustGet(taskId);
    if (!InspectionTaskStatus.PENDING.name().equals(task.getStatus())) {
      throw BizException.of(ErrorCodes.TASK_NOT_DISPATCHABLE,
          Formatters.format(ErrorMessages.TASK_NOT_DISPATCHABLE, task.getStatus()));
    }
    String today = LocalDate.now().toString();
    List<InspectorQualification> qualified =
        qualificationMapper.findValidByType(task.getInspectionType(), today);

    task.setDispatchAttempts(task.getDispatchAttempts() == null ? 1 : task.getDispatchAttempts() + 1);
    task.setUpdatedAt(Formatters.now());

    if (qualified.isEmpty()) {
      // 派发失败：记录原因，任务保持 PENDING，可重试
      String reason = Formatters.format(ErrorMessages.TASK_DISPATCH_FAILED, task.getInspectionType());
      task.setLastDispatchError(reason);
      taskMapper.updateById(task);
      log.warn(Formatters.audit(LogTemplates.TASK_DISPATCH_FAILED, task.getTaskNo(),
          task.getInspectionType(), task.getDispatchAttempts(), reason));
      throw BizException.retryable(ErrorCodes.TASK_DISPATCH_FAILED, reason);
    }

    task.setStatus(InspectionTaskStatus.POOLED.name());
    task.setDispatchedAt(Formatters.now());
    task.setLastDispatchError(null);
    taskMapper.updateById(task);
    log.info(Formatters.audit(LogTemplates.TASK_DISPATCH, task.getTaskNo(),
        task.getInspectionType(), qualified.size(), operator));
    return task;
  }

  /**
   * 领取任务（先到先得）。
   *
   * <p>两名检验员同时领取同一任务时，由数据库原子条件 UPDATE（{@code WHERE status='POOLED'}）
   * 保证只有先到的一方命中行，另一方返回 0 并收到「已被领走」。领取人必须持有该检验类型的
   * 有效资质，否则按越权领取直接拒绝。</p>
   */
  @Transactional
  public InspectionTask claim(Long taskId, Long inspectorId) {
    InspectionTask task = mustGet(taskId);
    AppUser inspector = userMapper.selectById(inspectorId);
    if (inspector == null) {
      throw BizException.of(ErrorCodes.INSPECTOR_NOT_FOUND,
          Formatters.format(ErrorMessages.INSPECTOR_NOT_FOUND, String.valueOf(inspectorId)));
    }

    // 越权领取直接拒绝：不具备该类型有效资质
    InspectionType type = InspectionType.from(task.getInspectionType());
    if (!qualificationService.isQualified(inspectorId, type)) {
      String reason = Formatters.format(ErrorMessages.TASK_CLAIM_FORBIDDEN, type.label());
      log.warn(Formatters.audit(LogTemplates.TASK_CLAIM_REJECTED, task.getTaskNo(),
          inspector.getDisplayName(), task.getInspectionType(), reason));
      throw BizException.of(ErrorCodes.TASK_CLAIM_FORBIDDEN, reason);
    }

    if (!InspectionTaskStatus.POOLED.name().equals(task.getStatus())) {
      throw BizException.of(ErrorCodes.TASK_NOT_CLAIMABLE,
          Formatters.format(ErrorMessages.TASK_NOT_CLAIMABLE, task.getStatus()));
    }

    // 先到先得：数据库级原子条件更新。0 行说明已被他人领走（或状态已变）。
    String now = Formatters.now();
    int rows;
    try {
      rows = taskMapper.claimTask(taskId, inspectorId, now);
    } catch (DuplicateKeyException e) {
      rows = 0;
    }
    if (rows == 0) {
      log.warn(Formatters.audit(LogTemplates.TASK_CLAIM_RACE_LOST, task.getTaskNo(),
              inspector.getDisplayName()));
      throw BizException.of(ErrorCodes.TASK_ALREADY_CLAIMED,
          Formatters.format(ErrorMessages.TASK_ALREADY_CLAIMED, task.getTaskNo()));
    }
    task.setStatus(InspectionTaskStatus.CLAIMED.name());
    task.setAssigneeId(inspectorId);
    task.setClaimedAt(now);
    log.info(Formatters.audit(LogTemplates.TASK_CLAIM, task.getTaskNo(),
        inspector.getDisplayName(), task.getInspectionType()));
    return task;
  }

  /**
   * 提交检验结论。仅接单人本人可提交；提交时固化检验员与资质快照，
   * 此后即便资质过期/吊销，已提交结论依旧有效。
   */
  @Transactional
  public QualityInspection submit(Long taskId, Long inspectorId, String resultStatus,
                                  String standardVersion) {
    InspectionTask task = mustGet(taskId);
    if (InspectionTaskStatus.SUBMITTED.name().equals(task.getStatus())) {
      throw BizException.of(ErrorCodes.TASK_ALREADY_SUBMITTED,
          Formatters.format(ErrorMessages.TASK_ALREADY_SUBMITTED, task.getTaskNo()));
    }
    if (!InspectionTaskStatus.CLAIMED.name().equals(task.getStatus())) {
      throw BizException.of(ErrorCodes.TASK_NOT_CLAIMABLE,
          Formatters.format(ErrorMessages.TASK_NOT_CLAIMABLE, task.getStatus()));
    }
    if (!inspectorId.equals(task.getAssigneeId())) {
      throw BizException.of(ErrorCodes.TASK_SUBMIT_FORBIDDEN,
          Formatters.format(ErrorMessages.TASK_SUBMIT_FORBIDDEN, task.getTaskNo()));
    }

    AppUser inspector = userMapper.selectById(inspectorId);
    InspectionType type = InspectionType.from(task.getInspectionType());

    // 固化资质快照（提交时的资质，用于追溯「当时的资质」）
    InspectorQualification qual = qualificationMapper.findByInspectorAndType(inspectorId, type.name());
    String snapshot = buildQualificationSnapshot(type, qual);

    QualityInspection inspection = new QualityInspection();
    inspection.setBatchId(task.getBatchId());
    inspection.setTaskId(taskId);
    inspection.setInspectorId(inspectorId);
    inspection.setInspectorName(inspector == null ? String.valueOf(inspectorId) : inspector.getDisplayName());
    inspection.setInspectionType(type.name());
    inspection.setStandardVersion(standardVersion);
    inspection.setResultStatus(resultStatus);
    inspection.setQualificationId(qual == null ? null : qual.getId());
    inspection.setQualificationSnapshot(snapshot);
    inspection.setInspectedAt(Formatters.now());
    inspectionMapper.insert(inspection);

    task.setStatus(InspectionTaskStatus.SUBMITTED.name());
    task.setSubmittedAt(Formatters.now());
    task.setUpdatedAt(Formatters.now());
    taskMapper.updateById(task);

    log.info(Formatters.audit(LogTemplates.TASK_SUBMIT, task.getTaskNo(),
        inspection.getInspectorName(), resultStatus));
    log.info(Formatters.audit(LogTemplates.INSPECTION_SUBMIT, String.valueOf(task.getBatchId()),
        inspection.getInspectorName(), type.name(), resultStatus));
    return inspection;
  }

  /**
   * 资质过期/吊销后重新确认接单人：把该检验员名下「已接单未提交」且资质已失效的任务退回待派。
   * 已提交(SUBMITTED)的任务不动，结论照旧有效。
   *
   * @return 退回待派的任务数
   */
  @Transactional
  public int revertTasksForInspector(Long inspectorId) {
    List<InspectionTask> claimed = taskMapper.findByAssignee(inspectorId);
    int reverted = 0;
    for (InspectionTask task : claimed) {
      if (!InspectionTaskStatus.CLAIMED.name().equals(task.getStatus())) {
        continue;
      }
      InspectionType type = InspectionType.from(task.getInspectionType());
      if (qualificationService.isQualified(inspectorId, type)) {
        continue;
      }
      // 资质已失效：退回待派，清空接单人
      task.setStatus(InspectionTaskStatus.PENDING.name());
      task.setAssigneeId(null);
      task.setClaimedAt(null);
      task.setUpdatedAt(Formatters.now());
      int rows = taskMapper.updateById(task);
      if (rows > 0) {
        reverted++;
        log.info(Formatters.audit(LogTemplates.TASK_EXPIRED_REVERT, task.getTaskNo(),
            String.valueOf(inspectorId), task.getInspectionType()));
      }
    }
    return reverted;
  }

  // ---- 查询 ----

  public InspectionTask mustGet(Long taskId) {
    InspectionTask task = taskMapper.selectById(taskId);
    if (task == null) {
      throw BizException.of(ErrorCodes.TASK_NOT_FOUND,
          Formatters.format(ErrorMessages.TASK_NOT_FOUND, String.valueOf(taskId)));
    }
    return task;
  }

  public List<InspectionTask> listByBatch(Long batchId) {
    return taskMapper.findByBatch(batchId);
  }

  public List<InspectionTask> listByStatus(String status) {
    if (status == null || status.isBlank()) {
      return taskMapper.selectList(new QueryWrapper<>());
    }
    return taskMapper.findByStatus(status.toUpperCase());
  }

  public List<InspectionTask> listByAssignee(Long inspectorId) {
    return taskMapper.findByAssignee(inspectorId);
  }

  // ---- 内部工具 ----

  private String buildQualificationSnapshot(InspectionType type, InspectorQualification qual) {
    if (qual == null) {
      return type.label() + "资质：无记录";
    }
    String until = qual.getQualifiedUntil() == null || qual.getQualifiedUntil().isBlank()
        ? "长期" : "有效期至 " + qual.getQualifiedUntil();
    String status = QualificationStatus.VALID.name().equals(qual.getStatus()) ? "有效" : qual.getStatus();
    return type.label() + "资质[" + status + "] " + until;
  }

  private String generateTaskNo() {
    String day = LocalDateTime.now().format(DAY);
    String prefix = "TASK-" + day + "-";
    long count = taskMapper.selectCount(new QueryWrapper<InspectionTask>()
        .likeRight("task_no", prefix));
    return prefix + String.format("%04d", count + 1);
  }
}
