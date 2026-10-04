package com.generated.qualityTrace.dispatch.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorMessages;
import com.generated.qualityTrace.dispatch.constants.QualificationStatus;
import com.generated.qualityTrace.dispatch.exceptions.DispatchBusinessException;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.repositories.InspectorQualificationRepository;
import com.generated.qualityTrace.dispatch.repositories.InspectorRepository;
import com.generated.qualityTrace.dispatch.validators.DispatchValidator;

/**
 * 检验员资质领域服务：
 * - 统一回答“某检验员对某检验类型现在是否有有效资质”；
 * - 派发、领取、提交、复检都走这里，杜绝“资质过期的人接单出结论”。
 */
@Service
public class InspectorQualificationService {

  private final InspectorRepository inspectorRepository;
  private final InspectorQualificationRepository qualificationRepository;
  private final DispatchValidator validator;

  public InspectorQualificationService(InspectorRepository inspectorRepository,
                                       InspectorQualificationRepository qualificationRepository,
                                       DispatchValidator validator) {
    this.inspectorRepository = inspectorRepository;
    this.qualificationRepository = qualificationRepository;
    this.validator = validator;
  }

  /** 按工号取检验员，不存在/停用分别抛 404/403。 */
  @Transactional(readOnly = true)
  public Inspector requireActiveInspector(String employeeNo) {
    if (employeeNo == null || employeeNo.isBlank()) {
      throw DispatchBusinessException.badRequest(
          DispatchErrorCodes.CALLER_REQUIRED, DispatchErrorMessages.CALLER_REQUIRED);
    }
    Inspector inspector = inspectorRepository.findByEmployeeNo(employeeNo)
        .orElseThrow(() -> DispatchBusinessException.notFound(
            DispatchErrorCodes.INSPECTOR_NOT_FOUND,
            String.format(DispatchErrorMessages.INSPECTOR_NOT_FOUND, employeeNo)));
    if (!inspector.isActive()) {
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.INSPECTOR_INACTIVE,
          String.format(DispatchErrorMessages.INSPECTOR_INACTIVE, employeeNo));
    }
    return inspector;
  }

  /** 按 id 取检验员（不存在返回 empty，用于组装领取人信息）。 */
  @Transactional(readOnly = true)
  public Optional<Inspector> findInspectorById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return inspectorRepository.findById(id);
  }

  /** 按 id 取在岗检验员，派发成功后记录日志用。 */
  @Transactional(readOnly = true)
  public Inspector requireActiveInspectorById(Long id) {
    Inspector inspector = inspectorRepository.findById(id)
        .orElseThrow(() -> DispatchBusinessException.notFound(
            DispatchErrorCodes.INSPECTOR_NOT_FOUND,
            String.format(DispatchErrorMessages.INSPECTOR_NOT_FOUND, String.valueOf(id))));
    if (!inspector.isActive()) {
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.INSPECTOR_INACTIVE,
          String.format(DispatchErrorMessages.INSPECTOR_INACTIVE, inspector.getEmployeeNo()));
    }
    return inspector;
  }

  /**
   * 返回检验员对某检验类型当前有效的资质；不存在有效资质时返回 empty。
   * 调用方按场景决定是拒绝（领取）还是记派发失败（派发）。
   */
  @Transactional(readOnly = true)
  public Optional<InspectorQualification> findValidQualification(Long inspectorId, String inspectionType) {
    LocalDate today = validator.today();
    return qualificationRepository
        .findByInspectorIdAndInspectionType(inspectorId, inspectionType)
        .stream()
        .filter(q -> q.isValidOn(today))
        .findFirst();
  }

  /**
   * 领取/提交场景的硬性门禁：必须持有该类型的有效资质，否则按原因抛 403，
   * 越权领取直接拒绝。
   */
  @Transactional(readOnly = true)
  public InspectorQualification requireValidQualification(Inspector inspector, String inspectionType) {
    LocalDate today = validator.today();
    List<InspectorQualification> all =
        qualificationRepository.findByInspectorIdAndInspectionType(inspector.getId(), inspectionType);

    InspectorQualification active = null;
    for (InspectorQualification q : all) {
      if (QualificationStatus.ACTIVE.name().equals(q.getStatus())) {
        active = q;
        if (!today.isBefore(q.getValidFrom()) && !today.isAfter(q.getValidUntil())) {
          return q;
        }
      }
    }
    if (active != null && active.getValidUntil().isBefore(today)) {
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.QUALIFICATION_EXPIRED,
          String.format(DispatchErrorMessages.QUALIFICATION_EXPIRED,
              inspector.getEmployeeNo(), inspectionType,
              active.getQualificationNo(), active.getValidUntil()));
    }
    boolean hasRevoked = all.stream()
        .anyMatch(q -> QualificationStatus.REVOKED.name().equals(q.getStatus()));
    if (hasRevoked) {
      throw DispatchBusinessException.forbidden(
          DispatchErrorCodes.QUALIFICATION_REVOKED,
          String.format(DispatchErrorMessages.QUALIFICATION_REVOKED,
              inspector.getEmployeeNo(),
              all.stream().filter(q -> QualificationStatus.REVOKED.name().equals(q.getStatus()))
                  .findFirst().get().getQualificationNo()));
    }
    throw DispatchBusinessException.forbidden(
        DispatchErrorCodes.QUALIFICATION_MISSING,
        String.format(DispatchErrorMessages.QUALIFICATION_MISSING,
            inspector.getEmployeeNo(), inspectionType));
  }

  /**
   * 派发候选：某检验类型当前资质有效且检验员在岗的列表（资质到期日升序）。
   */
  @Transactional(readOnly = true)
  public List<InspectorQualification> findDispatchCandidates(String inspectionType) {
    LocalDate today = validator.today();
    return qualificationRepository
        .findByInspectionTypeAndStatusAndValidUntilGreaterThanEqualOrderByValidUntilAscIdAsc(
            inspectionType, QualificationStatus.ACTIVE.name(), today)
        .stream()
        .filter(q -> q.isValidOn(today))
        .filter(q -> inspectorRepository.findById(q.getInspectorId())
            .map(Inspector::isActive).orElse(false))
        .toList();
  }

  /** 按资质号取当前记录（追溯时用来判定“当时资质”现在 EXPIRED/REVOKED/VALID）。 */
  @Transactional(readOnly = true)
  public Optional<InspectorQualification> findByQualificationNo(String qualificationNo) {
    if (qualificationNo == null) {
      return Optional.empty();
    }
    return qualificationRepository.findByQualificationNo(qualificationNo);
  }
}
