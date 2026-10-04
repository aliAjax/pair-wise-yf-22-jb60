package com.generated.qualityTrace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.generated.qualityTrace.constants.InspectionTaskStatus;
import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constants.QualificationStatus;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.models.AppUser;
import com.generated.qualityTrace.models.InspectionTask;
import com.generated.qualityTrace.models.InspectorQualification;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.repositories.AppUserMapper;
import com.generated.qualityTrace.repositories.InspectorQualificationMapper;
import com.generated.qualityTrace.repositories.ProductBatchMapper;
import com.generated.qualityTrace.repositories.QualityInspectionMapper;
import com.generated.qualityTrace.services.InspectionTaskService;
import com.generated.qualityTrace.services.QualificationService;
import com.generated.qualityTrace.services.TraceService;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * 检验任务派发/领取/资质/追溯 集成测试（H2）。
 * 每个测试方法重建上下文，避免资质状态互相污染。
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InspectionTaskFlowTest {

  @Autowired private InspectionTaskService taskService;
  @Autowired private QualificationService qualificationService;
  @Autowired private TraceService traceService;
  @Autowired private AppUserMapper userMapper;
  @Autowired private InspectorQualificationMapper qualificationMapper;
  @Autowired private ProductBatchMapper batchMapper;
  @Autowired private QualityInspectionMapper inspectionMapper;
  @Autowired private com.generated.qualityTrace.repositories.InspectionTaskMapper taskMapper;

  private Long zhangId;
  private Long liId;
  private Long batchId;

  @BeforeEach
  void setUp() {
    zhangId = userMapper.findByUsername("inspector.zhang").getId();
    liId = userMapper.findByUsername("inspector.li").getId();
    batchId = batchMapper.findByBatchNo("BATCH-20261004-001").getId();
  }

  /** happy path：派发 → 领取 → 提交，结论含检验员与资质快照。 */
  @Test
  void dispatch_claim_submit_happy_path() {
    InspectionTask task = taskService.createTask(batchId, InspectionType.FIRST_INSPECTION, "王主管");
    task = taskService.dispatch(task.getId(), "王主管");
    assertEquals(InspectionTaskStatus.POOLED.name(), task.getStatus());

    task = taskService.claim(task.getId(), zhangId);
    assertEquals(InspectionTaskStatus.CLAIMED.name(), task.getStatus());
    assertEquals(zhangId, task.getAssigneeId());

    QualityInspection inspection = taskService.submit(task.getId(), zhangId, "PASS", "V1.0");
    assertNotNull(inspection.getId());
    assertEquals(zhangId, inspection.getInspectorId());
    assertNotNull(inspection.getQualificationId());
    assertNotNull(inspection.getQualificationSnapshot());
    assertTrue(inspection.getQualificationSnapshot().contains("首检"));

    // 任务已提交
    InspectionTask done = taskService.mustGet(task.getId());
    assertEquals(InspectionTaskStatus.SUBMITTED.name(), done.getStatus());

    // 追溯返回检验员与当时资质
    Map<String, Object> trace = traceService.traceByBatchNo("BATCH-20261004-001", "孙审计");
    assertEquals(1, trace.get("taskCount"));
    @SuppressWarnings("unchecked")
    Map<String, Object> taskView = ((java.util.List<Map<String, Object>>) trace.get("tasks")).get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> conclusion = (Map<String, Object>) taskView.get("conclusion");
    assertEquals("张质检", conclusion.get("inspectorName"));
    assertTrue(conclusion.get("qualificationSnapshot").toString().contains("首检"));
  }

  /** 并发领取：两名检验员同时领取同一任务，只有先到的拿到（数据库级原子保证）。 */
  @Test
  void concurrent_claim_first_wins() throws Exception {
    // 给李质检也授予首检资质，使两人都可领
    qualificationService.grant(liId, InspectionType.FIRST_INSPECTION, "2026-01-01", "2099-12-31", "赵经理");

    InspectionTask task = taskService.createTask(batchId, InspectionType.FIRST_INSPECTION, "王主管");
    task = taskService.dispatch(task.getId(), "王主管");
    Long taskId = task.getId();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger zhangWins = new AtomicInteger();
    AtomicInteger liWins = new AtomicInteger();

    // 两个线程同时对同一待领任务发起原子领取
    Runnable claimZhang = () -> {
      try {
        start.await();
        int rows = taskMapper.claimTask(taskId, zhangId, com.generated.qualityTrace.utils.Formatters.now());
        if (rows == 1) {
          zhangWins.incrementAndGet();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };
    Runnable claimLi = () -> {
      try {
        start.await();
        int rows = taskMapper.claimTask(taskId, liId, com.generated.qualityTrace.utils.Formatters.now());
        if (rows == 1) {
          liWins.incrementAndGet();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };

    Future<?> f1 = pool.submit(claimZhang);
    Future<?> f2 = pool.submit(claimLi);
    start.countDown();
    f1.get();
    f2.get();
    pool.shutdown();

    int totalWins = zhangWins.get() + liWins.get();
    assertEquals(1, totalWins, "两名检验员同时领取，有且仅有一人能拿到任务");

    // 最终任务属于先到的那一方
    InspectionTask claimed = taskService.mustGet(taskId);
    assertEquals(InspectionTaskStatus.CLAIMED.name(), claimed.getStatus());
    assertTrue(claimed.getAssigneeId().equals(zhangId) || claimed.getAssigneeId().equals(liId));
  }

  /** 越权领取直接拒绝：不具备该类型资质。 */
  @Test
  void claim_without_qualification_rejected() {
    // 授予李质检巡检资质，使巡检任务可被派发（进入待领池）
    qualificationService.grant(liId, InspectionType.PATROL_INSPECTION, "2026-01-01", "2099-12-31", "赵经理");

    InspectionTask task = taskService.createTask(batchId, InspectionType.PATROL_INSPECTION, "王主管");
    task = taskService.dispatch(task.getId(), "王主管");
    final Long taskId = task.getId();
    assertEquals(InspectionTaskStatus.POOLED.name(), task.getStatus());

    // 张质检无巡检资质，越权领取直接拒绝
    BizException ex = assertThrows(BizException.class, () -> taskService.claim(taskId, zhangId));
    assertEquals("TASK_CLAIM_FORBIDDEN", ex.getCode());
    // 任务仍在待领池，未被领走
    assertEquals(InspectionTaskStatus.POOLED.name(), taskService.mustGet(taskId).getStatus());
    // 李质检（有资质）可以正常领取
    InspectionTask claimed = taskService.claim(taskId, liId);
    assertEquals(InspectionTaskStatus.CLAIMED.name(), claimed.getStatus());
  }

  /** 派发失败可重试：无资质检验员时失败，授予资质后重试成功。 */
  @Test
  void dispatch_failure_retryable_then_success() {
    InspectionTask task = taskService.createTask(batchId, InspectionType.PATROL_INSPECTION, "王主管");

    BizException ex = assertThrows(BizException.class,
        () -> taskService.dispatch(task.getId(), "王主管"));
    assertEquals("TASK_DISPATCH_FAILED", ex.getCode());
    assertTrue(ex.isRetryable(), "派发失败应可重试");
    // 任务仍停留在待派
    assertEquals(InspectionTaskStatus.PENDING.name(), taskService.mustGet(task.getId()).getStatus());

    // 授予李质检巡检资质后重试
    qualificationService.grant(liId, InspectionType.PATROL_INSPECTION, "2026-01-01", "2099-12-31", "赵经理");
    InspectionTask retried = taskService.dispatch(task.getId(), "王主管");
    assertEquals(InspectionTaskStatus.POOLED.name(), retried.getStatus());
    assertTrue(retried.getDispatchAttempts() >= 2, "重试后派发次数应累加");
  }

  /** 资质过期：已接单未提交任务退回待派；已提交结论照旧有效。 */
  @Test
  void qualification_expiry_reverts_unsubmitted_keeps_submitted() {
    // 任务1：派发并领取（不提交）
    InspectionTask t1 = taskService.createTask(batchId, InspectionType.FIRST_INSPECTION, "王主管");
    t1 = taskService.dispatch(t1.getId(), "王主管");
    t1 = taskService.claim(t1.getId(), zhangId);
    assertEquals(InspectionTaskStatus.CLAIMED.name(), t1.getStatus());

    // 任务2：派发、领取并提交
    InspectionTask t2 = taskService.createTask(batchId, InspectionType.FINAL_INSPECTION, "王主管");
    t2 = taskService.dispatch(t2.getId(), "王主管");
    t2 = taskService.claim(t2.getId(), zhangId);
    QualityInspection submitted = taskService.submit(t2.getId(), zhangId, "PASS", "V1.0");
    assertEquals(InspectionTaskStatus.SUBMITTED.name(), taskService.mustGet(t2.getId()).getStatus());

    // 把张质检的首检/终检资质都置为过期
    expireQualification(zhangId, InspectionType.FIRST_INSPECTION);
    expireQualification(zhangId, InspectionType.FINAL_INSPECTION);

    int reverted = qualificationService.revalidateExpired();
    assertTrue(reverted >= 1, "至少退回 1 个已接单未提交任务");

    // 任务1 退回待派
    InspectionTask after1 = taskService.mustGet(t1.getId());
    assertEquals(InspectionTaskStatus.PENDING.name(), after1.getStatus());
    // 任务2 已提交，结论照旧有效
    InspectionTask after2 = taskService.mustGet(t2.getId());
    assertEquals(InspectionTaskStatus.SUBMITTED.name(), after2.getStatus());
    QualityInspection kept = inspectionMapper.selectById(submitted.getId());
    assertNotNull(kept);
    assertEquals("PASS", kept.getResultStatus());
  }

  /** 提交后即便资质被吊销，追溯仍返回当时的检验员与资质快照。 */
  @Test
  void trace_keeps_snapshot_after_revoke() {
    InspectionTask task = taskService.createTask(batchId, InspectionType.FIRST_INSPECTION, "王主管");
    task = taskService.dispatch(task.getId(), "王主管");
    task = taskService.claim(task.getId(), zhangId);
    QualityInspection inspection = taskService.submit(task.getId(), zhangId, "PASS", "V1.0");

    // 吊销资质
    InspectorQualification q = qualificationMapper.findByInspectorAndType(zhangId, "FIRST_INSPECTION");
    qualificationService.revoke(q.getId(), "赵经理");

    // 追溯仍能看到提交时的检验员与资质
    Map<String, Object> trace = traceService.traceByBatchNo("BATCH-20261004-001", "孙审计");
    @SuppressWarnings("unchecked")
    Map<String, Object> taskView = ((java.util.List<Map<String, Object>>) trace.get("tasks")).get(0);
    @SuppressWarnings("unchecked")
    Map<String, Object> conclusion = (Map<String, Object>) taskView.get("conclusion");
    assertEquals("张质检", conclusion.get("inspectorName"));
    assertNotNull(conclusion.get("qualificationSnapshot"));
    assertEquals(inspection.getId(), conclusion.get("inspectionId"));
  }

  /** 把资质截止日期改为过去（模拟时间流逝），状态仍保持 VALID，等待批处理扫描。 */
  private void expireQualification(Long inspectorId, InspectionType type) {
    InspectorQualification q = qualificationMapper.findByInspectorAndType(inspectorId, type.name());
    q.setQualifiedUntil("2020-01-01");
    qualificationMapper.updateById(q);
  }
}
