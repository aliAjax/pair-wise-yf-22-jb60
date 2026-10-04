package com.generated.qualityTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.generated.qualityTrace.dispatch.constants.InspectionTaskStatus;
import com.generated.qualityTrace.dispatch.exceptions.DispatchBusinessException;
import com.generated.qualityTrace.dispatch.services.InspectionTaskService;
import com.generated.qualityTrace.dispatch.services.QualificationRecheckService;
import com.generated.qualityTrace.dispatch.services.BatchTraceService;
import com.generated.qualityTrace.dispatch.types.BatchTraceView;
import com.generated.qualityTrace.dispatch.types.CreateInspectionTaskPayload;
import com.generated.qualityTrace.dispatch.types.InspectionTaskView;
import com.generated.qualityTrace.dispatch.types.QualificationRecheckView;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 检验任务派发全链路集成测试：
 * 按类型派发、并发先到先得、越权/过期拒绝、派发失败重试、
 * 资质过期退回待派/已提交保持有效、按批号追溯检验员与当时资质。
 */
@SpringBootTest
class InspectionDispatchIntegrationTest {

  @Autowired private InspectionTaskService taskService;
  @Autowired private QualificationRecheckService recheckService;
  @Autowired private BatchTraceService traceService;
  @Autowired private JdbcTemplate jdbc;

  private static final String BATCH = "B-TEST-001";

  @BeforeEach
  void seed() {
    jdbc.update("DELETE FROM inspection_task");
    jdbc.update("DELETE FROM inspector_qualification");
    jdbc.update("DELETE FROM inspector");
    jdbc.update("DELETE FROM audit_log");
    jdbc.update("ALTER TABLE inspection_task ALTER COLUMN id RESTART WITH 1");

    // E001 首检有效；E002 首检有效（用于并发争抢）；E003 仅终检；E004 首检已过期；E005 已停用
    jdbc.update("INSERT INTO inspector(employee_no,name,role,active) VALUES "
        + "('E001','张三','INSPECTOR',TRUE),"
        + "('E002','李四','INSPECTOR',TRUE),"
        + "('E003','王五','INSPECTOR',TRUE),"
        + "('E004','赵六','INSPECTOR',TRUE),"
        + "('E005','孙七','INSPECTOR',FALSE)");
    Long id1 = inspectorId("E001");
    Long id2 = inspectorId("E002");
    Long id3 = inspectorId("E003");
    Long id4 = inspectorId("E004");
    Long id5 = inspectorId("E005");
    java.time.LocalDate now = java.time.LocalDate.now();
    addQualification(id1, "Q-FIRST-001", "FIRST_INSPECTION", now.minusDays(30), now.plusDays(365), "ACTIVE");
    addQualification(id2, "Q-FIRST-002", "FIRST_INSPECTION", now.minusDays(30), now.plusDays(365), "ACTIVE");
    addQualification(id3, "Q-FINAL-003", "FINAL_INSPECTION", now.minusDays(30), now.plusDays(365), "ACTIVE");
    // 已过期
    addQualification(id4, "Q-FIRST-004", "FIRST_INSPECTION", now.minusYears(2), now.minusDays(1), "ACTIVE");
    addQualification(id5, "Q-FIRST-005", "FIRST_INSPECTION", now.minusDays(30), now.plusDays(365), "ACTIVE");
    // 巡检类型初始无任何人有资质（用于派发失败->重试场景）
  }

  private void addQualification(Long inspectorId, String no, String type,
                                java.time.LocalDate from, java.time.LocalDate until, String status) {
    jdbc.update("INSERT INTO inspector_qualification"
        + "(inspector_id,qualification_no,inspection_type,valid_from,valid_until,status) "
        + "VALUES (?,?,?,?,?,?)",
        inspectorId, no, type, java.sql.Date.valueOf(from), java.sql.Date.valueOf(until), status);
  }

  private Long inspectorId(String employeeNo) {
    return jdbc.queryForObject("SELECT id FROM inspector WHERE employee_no=?", Long.class, employeeNo);
  }

  private CreateInspectionTaskPayload taskPayload(String type, boolean autoDispatch) {
    CreateInspectionTaskPayload p = new CreateInspectionTaskPayload();
    p.setBatchNo(BATCH);
    p.setInspectionType(type);
    p.setStandardVersion("STD-v1");
    p.setAutoDispatch(autoDispatch);
    return p;
  }

  @Test
  void createWithAutoDispatch_assignsQualifiedInspectorOfMatchingType() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    assertEquals(InspectionTaskStatus.CLAIMED.name(), v.status);
    assertEquals("E001", v.assigneeEmployeeNo);
    assertEquals("Q-FIRST-001", v.claimedQualificationNo);
    assertNotNull(v.claimedAt);
  }

  @Test
  void concurrentClaim_onlyFirstComerWins() throws Exception {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", false), "SYS");
    assertEquals(InspectionTaskStatus.PENDING_DISPATCH.name(), v.status);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> results = new ArrayList<>();
    for (String caller : List.of("E001", "E002")) {
      results.add(pool.submit(() -> {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);
        try {
          return taskService.claim(v.taskNo, caller).assigneeEmployeeNo;
        } catch (DispatchBusinessException ex) {
          return "ERR:" + ex.getCode();
        }
      }));
    }
    ready.await(5, TimeUnit.SECONDS);
    start.countDown();

    List<String> outcomes = new ArrayList<>();
    for (Future<String> f : results) {
      outcomes.add(f.get(10, TimeUnit.SECONDS));
    }
    pool.shutdown();

    long winners = outcomes.stream().filter(o -> o.equals("E001") || o.equals("E002")).count();
    long losers = outcomes.stream().filter(o -> ("ERR:" + com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes.TASK_ALREADY_CLAIMED).equals(o)).count();
    assertEquals(1, winners, "必须恰好一人领取成功: " + outcomes);
    assertEquals(1, losers, "必须恰好一人因先到先得落败: " + outcomes);

    InspectionTaskView after = taskService.getView(v.taskNo);
    assertEquals(InspectionTaskStatus.CLAIMED.name(), after.status);
  }

  @Test
  void claimWithoutMatchingQualification_isRejectedAsIllegalClaim() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", false), "SYS");
    // E003 只有终检资质，越权领取首检任务
    DispatchBusinessException ex = assertThrows(DispatchBusinessException.class,
        () -> taskService.claim(v.taskNo, "E003"));
    assertEquals(403, ex.getHttpStatus());
    assertEquals(com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes.ILLEGAL_CLAIM, ex.getCode());
    // 任务仍在待派，没有被越权者占用
    assertEquals(InspectionTaskStatus.PENDING_DISPATCH.name(), taskService.getView(v.taskNo).status);
  }

  @Test
  void claimWithExpiredQualification_isRejected() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", false), "SYS");
    DispatchBusinessException ex = assertThrows(DispatchBusinessException.class,
        () -> taskService.claim(v.taskNo, "E004"));
    assertEquals(403, ex.getHttpStatus());
    assertNull(jdbc.queryForObject("SELECT assignee_id FROM inspection_task WHERE task_no=?",
        Long.class, v.taskNo), "过期资质的人不能占用任务");
  }

  @Test
  void inactiveInspectorCannotClaim() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", false), "SYS");
    DispatchBusinessException ex = assertThrows(DispatchBusinessException.class,
        () -> taskService.claim(v.taskNo, "E005"));
    assertEquals(403, ex.getHttpStatus());
  }

  @Test
  void dispatchFailsWhenNoQualifiedInspector_thenRetrySucceedsAfterQualificationAdded() {
    // 巡检类型无人有资质：自动派发失败，任务落 DISPATCH_FAILED 且记录原因/次数
    InspectionTaskView v = taskService.create(taskPayload("PATROL_INSPECTION", true), "SYS");
    assertEquals(InspectionTaskStatus.DISPATCH_FAILED.name(), v.status);
    assertTrue(v.dispatchAttempts >= 1);
    assertNotNull(v.lastDispatchError);

    // 直接调派发仍失败（可重试性：状态不会被破坏）
    DispatchBusinessException ex = assertThrows(DispatchBusinessException.class,
        () -> taskService.dispatch(v.taskNo, "SYS"));
    assertEquals(422, ex.getHttpStatus());

    // 给 E001 补上巡检资质后重试 -> 派发成功
    addQualification(inspectorId("E001"), "Q-PATROL-001", "PATROL_INSPECTION",
        java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now().plusDays(365), "ACTIVE");
    InspectionTaskView retried = taskService.retryDispatch(v.taskNo, "SYS");
    assertEquals(InspectionTaskStatus.CLAIMED.name(), retried.status);
    assertEquals("E001", retried.assigneeEmployeeNo);
  }

  @Test
  void submitByNonAssigneeRejected_andAssigneeSubmitStored() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    // 自动派发给 E001；E002 也有首检资质但不是领取人，提交被拒
    DispatchBusinessException ex = assertThrows(DispatchBusinessException.class,
        () -> taskService.submit(v.taskNo, "PASS", "别人提交", "E002"));
    assertEquals(403, ex.getHttpStatus());

    InspectionTaskView submitted = taskService.submit(v.taskNo, "PASS", "合格", "E001");
    assertEquals(InspectionTaskStatus.SUBMITTED.name(), submitted.status);
    assertEquals("PASS", submitted.resultStatus);
    assertNotNull(submitted.submittedAt);

    // 重复提交拒绝
    assertThrows(DispatchBusinessException.class,
        () -> taskService.submit(v.taskNo, "FAIL", null, "E001"));
  }

  @Test
  void recheckAfterQualificationExpiry_unsubmittedReturned_submittedKept() {
    // 任务1：已领取未提交
    InspectionTaskView unsubmitted = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    // 任务2：已领取并提交
    InspectionTaskView submitted = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    taskService.submit(submitted.taskNo, "FAIL", "不合格", submitted.assigneeEmployeeNo);

    // E001 的首检资质现在过期（模拟时间流逝，不重新发资质）
    jdbc.update("UPDATE inspector_qualification SET valid_until=CURRENT_DATE - 1 WHERE qualification_no='Q-FIRST-001'");

    QualificationRecheckView r = recheckService.recheck(BATCH, "QM");
    assertEquals(1, r.returnedToPending);
    assertEquals(1, r.submittedKeptValid);
    assertTrue(r.returnedTaskNos.contains(unsubmitted.taskNo));
    assertTrue(r.submittedTaskNos.contains(submitted.taskNo));

    // 未提交任务已退回待派、清空领取人和资质快照
    InspectionTaskView returned = taskService.getView(unsubmitted.taskNo);
    assertEquals(InspectionTaskStatus.PENDING_DISPATCH.name(), returned.status);
    assertNull(returned.assigneeId);
    assertNull(returned.claimedQualificationNo);

    // 已提交结论照旧有效：领取人与结论都在
    InspectionTaskView kept = taskService.getView(submitted.taskNo);
    assertEquals(InspectionTaskStatus.SUBMITTED.name(), kept.status);
    assertEquals("FAIL", kept.resultStatus);
    assertEquals("Q-FIRST-001", kept.claimedQualificationNo);

    // 退回后持有效资质的 E002 可以重新领取
    InspectionTaskView reclaimed = taskService.claim(unsubmitted.taskNo, "E002");
    assertEquals(InspectionTaskStatus.CLAIMED.name(), reclaimed.status);
    assertEquals("E002", reclaimed.assigneeEmployeeNo);
  }

  @Test
  void submitAfterClaimQualificationExpiry_returnsTaskToPendingAndRejectsConclusion() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    assertEquals("E001", v.assigneeEmployeeNo);

    // 领取后、提交前资质过期
    jdbc.update("UPDATE inspector_qualification SET valid_until=CURRENT_DATE - 1 WHERE qualification_no='Q-FIRST-001'");

    DispatchBusinessException ex = assertThrows(DispatchBusinessException.class,
        () -> taskService.submit(v.taskNo, "PASS", null, "E001"));
    assertEquals(403, ex.getHttpStatus());

    InspectionTaskView after = taskService.getView(v.taskNo);
    assertEquals(InspectionTaskStatus.PENDING_DISPATCH.name(), after.status, "未提交任务退回待派");
    assertNull(after.resultStatus, "过期资质产生的结论不得落库");
  }

  @Test
  void qualificationValidFromToday_isUsable_dispatchPicksIt() {
    // E002 的首检资质改成“今天才生效”，边界当天必须算有效；E001 资质昨天到期则不可派
    java.time.LocalDate now = java.time.LocalDate.now();
    jdbc.update("UPDATE inspector_qualification SET valid_from=?, valid_until=? WHERE qualification_no='Q-FIRST-002'",
        java.sql.Date.valueOf(now), java.sql.Date.valueOf(now.plusDays(10)));
    jdbc.update("UPDATE inspector_qualification SET valid_until=? WHERE qualification_no='Q-FIRST-001'",
        java.sql.Date.valueOf(now.minusDays(1)));

    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    assertEquals(InspectionTaskStatus.CLAIMED.name(), v.status, "今天生效的资质可派");
    assertEquals("E002", v.assigneeEmployeeNo);
    assertEquals("Q-FIRST-002", v.claimedQualificationNo);
  }

  @Test
  void traceByBatchNo_returnsInspectorAndQualificationAtClaim() {
    InspectionTaskView v = taskService.create(taskPayload("FIRST_INSPECTION", true), "SYS");
    taskService.submit(v.taskNo, "CONDITIONAL_PASS", "让步接收", "E001");

    BatchTraceView trace = traceService.trace(BATCH, "AUDITOR");
    assertEquals(1, trace.inspectionCount);
    assertEquals(1, trace.submittedCount);
    var node = trace.inspections.get(0);
    assertEquals("E001", node.inspectorEmployeeNo);
    assertEquals("张三", node.inspectorName);
    assertEquals("Q-FIRST-001", node.qualificationNoAtClaim, "返回当时领取所依据的资质");
    assertEquals("VALID", node.qualificationCurrentState);
    assertEquals("CONDITIONAL_PASS", node.resultStatus);

    // 资质事后过期：结论不变，追溯显示该资质当前 EXPIRED，但当时快照仍可还原
    jdbc.update("UPDATE inspector_qualification SET valid_until=CURRENT_DATE - 1 WHERE qualification_no='Q-FIRST-001'");
    BatchTraceView traceAfter = traceService.trace(BATCH, "AUDITOR");
    assertEquals("EXPIRED", traceAfter.inspections.get(0).qualificationCurrentState);
    assertEquals("Q-FIRST-001", traceAfter.inspections.get(0).qualificationNoAtClaim);
    assertEquals("CONDITIONAL_PASS", traceAfter.inspections.get(0).resultStatus, "已提交结论照旧有效");
  }
}
