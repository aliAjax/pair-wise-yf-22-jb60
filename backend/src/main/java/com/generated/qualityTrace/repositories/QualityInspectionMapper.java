package com.generated.qualityTrace.repositories;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.generated.qualityTrace.models.QualityInspection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QualityInspectionMapper extends BaseMapper<QualityInspection> {

  @Select("SELECT * FROM quality_inspection WHERE batch_id = #{batchId} ORDER BY id")
  List<QualityInspection> findByBatch(@Param("batchId") Long batchId);

  @Select("SELECT * FROM quality_inspection WHERE task_id = #{taskId} LIMIT 1")
  QualityInspection findByTask(@Param("taskId") Long taskId);
}
