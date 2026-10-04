package com.generated.qualityTrace.repositories;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.generated.qualityTrace.models.InspectionTask;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InspectionTaskMapper extends BaseMapper<InspectionTask> {

  @Select("SELECT * FROM inspection_task WHERE task_no = #{taskNo} LIMIT 1")
  InspectionTask findByTaskNo(@Param("taskNo") String taskNo);

  @Select("SELECT * FROM inspection_task WHERE batch_id = #{batchId} ORDER BY id")
  List<InspectionTask> findByBatch(@Param("batchId") Long batchId);

  @Select("SELECT * FROM inspection_task WHERE status = #{status} ORDER BY id")
  List<InspectionTask> findByStatus(@Param("status") String status);

  @Select("SELECT * FROM inspection_task WHERE assignee_id = #{assigneeId} ORDER BY id")
  List<InspectionTask> findByAssignee(@Param("assigneeId") Long assigneeId);

  /**
   * 原子领取（先到先得）：仅当任务仍处于待领(POOLED)状态时才把接单人改为领取人。
   *
   * <p>两名检验员同时领取同一任务时，数据库保证只有一个 UPDATE 能命中行（返回 1），
   * 另一方返回 0 —— 这是「只有先到的拿到」的数据库级保证，不依赖读取时序。</p>
   *
   * @return 受影响行数：1 表示领取成功，0 表示已被他人领走或状态已变
   */
  @Update("UPDATE inspection_task SET status = 'CLAIMED', assignee_id = #{assigneeId}, "
      + "claimed_at = #{now}, updated_at = #{now}, version = version + 1 "
      + "WHERE id = #{taskId} AND status = 'POOLED'")
  int claimTask(@Param("taskId") Long taskId,
                @Param("assigneeId") Long assigneeId,
                @Param("now") String now);
}
