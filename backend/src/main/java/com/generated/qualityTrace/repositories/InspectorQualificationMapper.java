package com.generated.qualityTrace.repositories;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.generated.qualityTrace.models.InspectorQualification;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InspectorQualificationMapper extends BaseMapper<InspectorQualification> {

  /** 某检验员某类型的资质（不限状态），用于判重与展示。 */
  @Select("SELECT * FROM inspector_qualification "
      + "WHERE inspector_id = #{inspectorId} AND inspection_type = #{inspectionType} LIMIT 1")
  InspectorQualification findByInspectorAndType(@Param("inspectorId") Long inspectorId,
                                                 @Param("inspectionType") String inspectionType);

  /** 某检验员全部资质。 */
  @Select("SELECT * FROM inspector_qualification WHERE inspector_id = #{inspectorId} "
      + "ORDER BY inspection_type")
  List<InspectorQualification> findByInspector(@Param("inspectorId") Long inspectorId);

  /**
   * 某检验类型下所有「有效」资质：状态 VALID，且未到截止日期（qualified_until 为空表示长期）。
   * 用于派发时筛选可接单人。
   */
  @Select("SELECT q.* FROM inspector_qualification q "
      + "JOIN app_user u ON u.id = q.inspector_id "
      + "WHERE q.inspection_type = #{inspectionType} "
      + "AND q.status = 'VALID' "
      + "AND u.status = 'ACTIVE' "
      + "AND (q.qualified_until IS NULL OR q.qualified_until >= #{today}) "
      + "ORDER BY q.inspector_id")
  List<InspectorQualification> findValidByType(@Param("inspectionType") String inspectionType,
                                               @Param("today") String today);

  /** 全部已过期但状态仍为 VALID 的资质（批处理用）。 */
  @Select("SELECT * FROM inspector_qualification "
      + "WHERE status = 'VALID' AND qualified_until IS NOT NULL AND qualified_until < #{today}")
  List<InspectorQualification> findExpired(@Param("today") String today);
}
