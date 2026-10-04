package com.generated.qualityTrace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP 全链路冒烟：路由、X-Inspector-Id 认证、越权状态码、追溯响应。 */
@SpringBootTest
@AutoConfigureMockMvc
class DispatchHttpSmokeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void seed() {
    jdbc.update("DELETE FROM inspection_task");
    jdbc.update("DELETE FROM inspector_qualification");
    jdbc.update("DELETE FROM inspector");
    jdbc.update("DELETE FROM audit_log");
    jdbc.update("INSERT INTO inspector(employee_no,name,role,active) VALUES "
        + "('E001','张三','INSPECTOR',TRUE),('E002','李四','INSPECTOR',TRUE)");
    Long id1 = jdbc.queryForObject("SELECT id FROM inspector WHERE employee_no='E001'", Long.class);
    jdbc.update("INSERT INTO inspector_qualification"
        + "(inspector_id,qualification_no,inspection_type,valid_from,valid_until,status) "
        + "VALUES (?,?,?,?,CURRENT_DATE + 30,'ACTIVE')",
        id1, "Q-FIRST-001", "FIRST_INSPECTION",
        java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)));
  }

  @Test
  void fullHttpFlow() throws Exception {
    // 1. 未带头创建 -> 400
    mockMvc.perform(post("/api/dispatch/tasks")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batchNo\":\"B-HTTP-1\",\"inspection_type\":\"FIRST_INSPECTION\"}"
                .replace("inspection_type", "inspectionType")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CALLER_REQUIRED"));

    // 2. 带头创建但不自动派发 -> 201 PENDING_DISPATCH
    String body = mockMvc.perform(post("/api/dispatch/tasks")
            .header("X-Inspector-Id", "E001")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batchNo\":\"B-HTTP-1\",\"inspectionType\":\"FIRST_INSPECTION\",\"autoDispatch\":false}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_DISPATCH"))
        .andReturn().getResponse().getContentAsString();
    String taskNo = com.jayway.jsonpath.JsonPath.read(body, "$.taskNo");

    // 3. 无资质的 E002 领取 -> 403 ILLEGAL_CLAIM
    mockMvc.perform(post("/api/dispatch/tasks/" + taskNo + "/claim").header("X-Inspector-Id", "E002"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ILLEGAL_CLAIM"));

    // 4. 有资质的 E001 领取 -> 200
    mockMvc.perform(post("/api/dispatch/tasks/" + taskNo + "/claim").header("X-Inspector-Id", "E001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLAIMED"))
        .andExpect(jsonPath("$.claimedQualificationNo").value("Q-FIRST-001"));

    // 5. E002 抢已被领取的任务 -> 409
    mockMvc.perform(post("/api/dispatch/tasks/" + taskNo + "/claim").header("X-Inspector-Id", "E002"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TASK_ALREADY_CLAIMED"));

    // 6. E002 越权提交 -> 403
    mockMvc.perform(post("/api/dispatch/tasks/" + taskNo + "/submit")
            .header("X-Inspector-Id", "E002")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"resultStatus\":\"PASS\"}"))
        .andExpect(status().isForbidden());

    // 7. E001 提交非法结论 -> 400
    mockMvc.perform(post("/api/dispatch/tasks/" + taskNo + "/submit")
            .header("X-Inspector-Id", "E001")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"resultStatus\":\"MAYBE\"}"))
        .andExpect(status().isBadRequest());

    // 8. E001 正常提交 -> 200 SUBMITTED
    mockMvc.perform(post("/api/dispatch/tasks/" + taskNo + "/submit")
            .header("X-Inspector-Id", "E001")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"resultStatus\":\"PASS\",\"resultNote\":\"ok\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.resultStatus").value("PASS"));

    // 9. 按批号追溯 -> 检验员 + 当时资质
    mockMvc.perform(get("/api/trace/B-HTTP-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.batchNo").value("B-HTTP-1"))
        .andExpect(jsonPath("$.inspectionCount").value(1))
        .andExpect(jsonPath("$.submittedCount").value(1))
        .andExpect(jsonPath("$.inspections[0].inspectorEmployeeNo").value("E001"))
        .andExpect(jsonPath("$.inspections[0].qualificationNoAtClaim").value("Q-FIRST-001"))
        .andExpect(jsonPath("$.inspections[0].qualificationCurrentState").value("VALID"));

    // 10. 健康检查
    mockMvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }
}
