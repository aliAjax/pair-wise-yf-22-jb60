package com.generated.qualityTrace.dispatch.repositories;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.generated.qualityTrace.dispatch.models.InspectionTask;

public interface InspectionTaskRepository extends JpaRepository<InspectionTask, Long> {

  Optional<InspectionTask> findByTaskNo(String taskNo);

  /** 任务单号序列（多实例共享，保证单号不撞）。 */
  @Query(value = "SELECT nextval('task_no_seq')", nativeQuery = true)
  Long nextTaskNoSequence();

  List<InspectionTask> findByBatchNoOrderByInspectionTypeAscIdAsc(String batchNo);

  List<InspectionTask> findByStatusOrderByIdAsc(String status);

  /**
   * 原子领取：仅当任务处于待派状态且无人领取时才能把行更新为 CLAIMED。
   * 两名检验员并发提交时，数据库行锁串行化两条 UPDATE，第二条匹配到 0 行落败——先到先得。
   * 返回受影响行数：1=领取成功，0=已被他人先领。
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inspection_task
         SET status = 'CLAIMED',
             assignee_id = :inspectorId,
             claimed_at = :now,
             claimed_qualification_id = :qualificationId,
             claimed_qualification_no = :qualificationNo,
             claimed_qualification_valid_until = :validUntil,
             updated_at = :now
       WHERE id = :taskId
         AND status = 'PENDING_DISPATCH'
         AND assignee_id IS NULL
      """, nativeQuery = true)
  int claimIfPending(@Param("taskId") Long taskId,
                     @Param("inspectorId") Long inspectorId,
                     @Param("qualificationId") Long qualificationId,
                     @Param("qualificationNo") String qualificationNo,
                     @Param("validUntil") LocalDate validUntil,
                     @Param("now") Instant now);

  /**
   * 系统派发：语义同原子领取，只允许从待派/派发失败两种状态派发到指定检验员。
   * 已被领取的任务返回 0，杜绝派发覆盖先到先得的结果。
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inspection_task
         SET status = 'CLAIMED',
             assignee_id = :inspectorId,
             claimed_at = :now,
             claimed_qualification_id = :qualificationId,
             claimed_qualification_no = :qualificationNo,
             claimed_qualification_valid_until = :validUntil,
             dispatch_attempts = dispatch_attempts + 1,
             last_dispatch_error = NULL,
             updated_at = :now
       WHERE id = :taskId
         AND status IN ('PENDING_DISPATCH', 'DISPATCH_FAILED')
         AND assignee_id IS NULL
      """, nativeQuery = true)
  int dispatch(@Param("taskId") Long taskId,
               @Param("inspectorId") Long inspectorId,
               @Param("qualificationId") Long qualificationId,
               @Param("qualificationNo") String qualificationNo,
               @Param("validUntil") LocalDate validUntil,
               @Param("now") Instant now);

  /** 记录一次派发失败（仍然没有领取人时才允许），供重试。 */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inspection_task
         SET status = 'DISPATCH_FAILED',
             dispatch_attempts = dispatch_attempts + 1,
             last_dispatch_error = :reason,
             updated_at = :now
       WHERE id = :taskId
         AND status IN ('PENDING_DISPATCH', 'DISPATCH_FAILED')
         AND assignee_id IS NULL
      """, nativeQuery = true)
  int markDispatchFailed(@Param("taskId") Long taskId,
                         @Param("reason") String reason,
                         @Param("now") Instant now);

  /**
   * 资质过期后退回待派：只动“已领取但还没提交”的任务。
   * 已提交（SUBMITTED）的结论不匹配 WHERE，照旧有效。
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inspection_task
         SET status = 'PENDING_DISPATCH',
             assignee_id = NULL,
             claimed_at = NULL,
             claimed_qualification_id = NULL,
             claimed_qualification_no = NULL,
             claimed_qualification_valid_until = NULL,
             updated_at = :now
       WHERE id = :taskId
         AND status = 'CLAIMED'
         AND assignee_id = :inspectorId
      """, nativeQuery = true)
  int returnToPendingIfClaimed(@Param("taskId") Long taskId,
                               @Param("inspectorId") Long inspectorId,
                               @Param("now") Instant now);

  /** 提交检验结论：只有本人且仍处于 CLAIMED 才能提交。 */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inspection_task
         SET status = 'SUBMITTED',
             result_status = :resultStatus,
             result_note = :resultNote,
             submitted_at = :now,
             updated_at = :now
       WHERE id = :taskId
         AND status = 'CLAIMED'
         AND assignee_id = :inspectorId
      """, nativeQuery = true)
  int submitIfClaimedByCaller(@Param("taskId") Long taskId,
                              @Param("inspectorId") Long inspectorId,
                              @Param("resultStatus") String resultStatus,
                              @Param("resultNote") String resultNote,
                              @Param("now") Instant now);
}
