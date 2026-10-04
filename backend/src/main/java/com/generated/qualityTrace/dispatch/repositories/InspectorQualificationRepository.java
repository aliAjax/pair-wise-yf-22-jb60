package com.generated.qualityTrace.dispatch.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;

public interface InspectorQualificationRepository extends JpaRepository<InspectorQualification, Long> {

  /** 某人某类型的全部资质（含已过期/已吊销），业务层按日期判定。 */
  List<InspectorQualification> findByInspectorIdAndInspectionType(Long inspectorId, String inspectionType);

  /** 某人的全部资质。 */
  List<InspectorQualification> findByInspectorId(Long inspectorId);

  /** 按资质号查当前记录（追溯时还原“当时资质”现在的状态）。 */
  Optional<InspectorQualification> findByQualificationNo(String qualificationNo);

  /**
   * 派发候选池（数据库侧粗筛）：某检验类型下状态 ACTIVE 且到期日不早于今天的资质，
   * 按到期日升序（快到期的优先排），再按资质 id 稳定排序。
   * valid_from 是否已到由 service 用 isValidOn 精确判定，避免日期边界在派生查询里出错。
   */
  List<InspectorQualification> findByInspectionTypeAndStatusAndValidUntilGreaterThanEqualOrderByValidUntilAscIdAsc(
      String inspectionType, String status, LocalDate today);
}
