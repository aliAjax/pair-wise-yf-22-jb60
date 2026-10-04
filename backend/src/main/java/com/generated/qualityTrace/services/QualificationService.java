package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.constants.QualificationStatus;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.models.InspectorQualification;
import com.generated.qualityTrace.repositories.AppUserMapper;
import com.generated.qualityTrace.repositories.InspectorQualificationMapper;
import com.generated.qualityTrace.utils.Formatters;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检验员资质服务。负责资质的授予、吊销、有效性校验与到期重新确认。
 *
 * <p>资质是派发与领取的唯一依据：只有「状态 VALID 且在有效期内」的检验员才能被派单/接单。</p>
 */
@Service
public class QualificationService {

  private static final Logger log = LoggerFactory.getLogger(QualificationService.class);

  private final InspectorQualificationMapper qualificationMapper;
  private final AppUserMapper userMapper;
  private final InspectionTaskService taskService;

  public QualificationService(InspectorQualificationMapper qualificationMapper,
                              AppUserMapper userMapper,
                              @Lazy InspectionTaskService taskService) {
    this.qualificationMapper = qualificationMapper;
    this.userMapper = userMapper;
    this.taskService = taskService;
  }

  /** 今天 yyyy-MM-dd。 */
  public String today() {
    return LocalDate.now().toString();
  }

  /**
   * 判断检验员是否具备某类型的有效资质。
   */
  public boolean isQualified(Long inspectorId, InspectionType type) {
    return getValidQualification(inspectorId, type) != null;
  }

  /**
   * 取检验员某类型的有效资质；不存在或已过期/吊销返回 null。
   */
  public InspectorQualification getValidQualification(Long inspectorId, InspectionType type) {
    InspectorQualification q = qualificationMapper.findByInspectorAndType(inspectorId, type.name());
    if (q == null) {
      return null;
    }
    if (!QualificationStatus.VALID.name().equals(q.getStatus())) {
      return null;
    }
    if (q.getQualifiedUntil() != null && !q.getQualifiedUntil().isBlank()
        && q.getQualifiedUntil().compareTo(today()) < 0) {
      return null;
    }
    return q;
  }

  /**
   * 授予（或重新授予）资质。同一检验员同一类型只保留一条有效记录；
   * 已存在且被吊销/过期的，重新激活并更新有效期。
   */
  @Transactional
  public InspectorQualification grant(Long inspectorId, InspectionType type,
                                      String qualifiedFrom, String qualifiedUntil,
                                      String operator) {
    AppUser inspector = userMapper.selectById(inspectorId);
    if (inspector == null) {
      throw BizException.of(com.generated.qualityTrace.constants.ErrorCodes.INSPECTOR_NOT_FOUND,
          Formatters.format(com.generated.qualityTrace.constants.ErrorMessages.INSPECTOR_NOT_FOUND,
              String.valueOf(inspectorId)));
    }
    String from = (qualifiedFrom == null || qualifiedFrom.isBlank()) ? today() : qualifiedFrom;
    InspectorQualification existing = qualificationMapper.findByInspectorAndType(inspectorId, type.name());
    if (existing == null) {
      InspectorQualification q = new InspectorQualification();
      q.setInspectorId(inspectorId);
      q.setInspectionType(type.name());
      q.setQualifiedFrom(from);
      q.setQualifiedUntil(qualifiedUntil);
      q.setStatus(QualificationStatus.VALID.name());
      q.setGrantedBy(operator);
      q.setCreatedAt(Formatters.now());
      q.setUpdatedAt(Formatters.now());
      qualificationMapper.insert(q);
      log.info(Formatters.audit(LogTemplates.QUAL_GRANT, inspectorId, type.name(),
          String.valueOf(qualifiedUntil), operator));
      return q;
    }
    // 已存在：更新有效期并置为有效（即便是吊销/过期后重新授予）
    existing.setQualifiedFrom(from);
    existing.setQualifiedUntil(qualifiedUntil);
    existing.setStatus(QualificationStatus.VALID.name());
    existing.setGrantedBy(operator);
    existing.setUpdatedAt(Formatters.now());
    qualificationMapper.updateById(existing);
    log.info(Formatters.audit(LogTemplates.QUAL_GRANT, inspectorId, type.name(),
        String.valueOf(qualifiedUntil), operator));
    return existing;
  }

  /**
   * 吊销资质。吊销后该检验员不能再接新单；已提交结论不受影响。
   */
  @Transactional
  public InspectorQualification revoke(Long qualificationId, String operator) {
    InspectorQualification q = qualificationMapper.selectById(qualificationId);
    if (q == null) {
      throw BizException.of(com.generated.qualityTrace.constants.ErrorCodes.QUALIFICATION_NOT_FOUND,
          Formatters.format(com.generated.qualityTrace.constants.ErrorMessages.QUALIFICATION_NOT_FOUND,
              String.valueOf(qualificationId)));
    }
    q.setStatus(QualificationStatus.REVOKED.name());
    q.setUpdatedAt(Formatters.now());
    qualificationMapper.updateById(q);
    log.info(Formatters.audit(LogTemplates.QUAL_REVOKE, q.getInspectorId(), q.getInspectionType(), operator));
    // 重新确认接单人：把该检验员名下已接单未提交的任务退回待派
    int reverted = taskService.revertTasksForInspector(q.getInspectorId());
    log.info(Formatters.audit(LogTemplates.QUAL_REVALIDATE, reverted, operator));
    return q;
  }

  /**
   * 资质到期批处理：把所有状态仍 VALID 但已过截止日期的资质置为 EXPIRED，
   * 并把这些检验员已接单未提交的任务退回待派。已提交结论不动。
   *
   * @return 退回待派的任务数
   */
  @Transactional
  public int revalidateExpired() {
    List<InspectorQualification> expired = qualificationMapper.findExpired(today());
    int reverted = 0;
    for (InspectorQualification q : expired) {
      q.setStatus(QualificationStatus.EXPIRED.name());
      q.setUpdatedAt(Formatters.now());
      qualificationMapper.updateById(q);
      log.info(Formatters.audit(LogTemplates.QUAL_EXPIRE, q.getInspectorId(),
          q.getInspectionType(), String.valueOf(q.getQualifiedUntil())));
      reverted += taskService.revertTasksForInspector(q.getInspectorId());
    }
    log.info(Formatters.audit(LogTemplates.QUAL_REVALIDATE, reverted, "system"));
    return reverted;
  }

  /** 某检验员全部资质（展示用）。 */
  public List<InspectorQualification> listByInspector(Long inspectorId) {
    return qualificationMapper.findByInspector(inspectorId);
  }
}
