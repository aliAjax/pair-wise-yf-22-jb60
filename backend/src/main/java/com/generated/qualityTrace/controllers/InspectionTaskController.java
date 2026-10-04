package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constructors.InspectionTaskDtoFactory;
import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.middlewares.RequireRole;
import com.generated.qualityTrace.models.InspectionTask;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.routes.InspectionTaskRoutes;
import com.generated.qualityTrace.services.InspectionTaskService;
import com.generated.qualityTrace.types.CreateTaskPayload;
import com.generated.qualityTrace.types.SubmitInspectionPayload;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检验任务控制器。派发/重试面向主管，领取/提交面向质检员，查询按角色开放。
 */
@RestController
public class InspectionTaskController {

  private final InspectionTaskService taskService;

  public InspectionTaskController(InspectionTaskService taskService) {
    this.taskService = taskService;
  }

  /** 创建检验任务（主管/经理）。 */
  @PostMapping(InspectionTaskRoutes.BASE)
  @RequireRole({ActorRole.SUPERVISOR, ActorRole.MANAGER})
  public Map<String, Object> create(@RequestBody CreateTaskPayload payload) {
    InspectionType type = parseType(payload.inspectionType());
    InspectionTask task = taskService.createTask(payload.batchId(), type, operator());
    return InspectionTaskDtoFactory.create(task);
  }

  /** 派发任务到有资质的检验员池；失败时 retryable=true，可重试（主管/经理）。 */
  @PostMapping(InspectionTaskRoutes.BASE + InspectionTaskRoutes.DISPATCH)
  @RequireRole({ActorRole.SUPERVISOR, ActorRole.MANAGER})
  public Map<String, Object> dispatch(@PathVariable Long id) {
    InspectionTask task = taskService.dispatch(id, operator());
    return InspectionTaskDtoFactory.create(task);
  }

  /** 领取任务（质检员，先到先得；越权领取直接拒绝）。 */
  @PostMapping(InspectionTaskRoutes.BASE + InspectionTaskRoutes.CLAIM)
  @RequireRole(ActorRole.INSPECTOR)
  public Map<String, Object> claim(@PathVariable Long id) {
    InspectionTask task = taskService.claim(id, LoginContext.require().id());
    return InspectionTaskDtoFactory.create(task);
  }

  /** 提交检验结论（质检员本人）。 */
  @PostMapping(InspectionTaskRoutes.BASE + InspectionTaskRoutes.SUBMIT)
  @RequireRole(ActorRole.INSPECTOR)
  public Map<String, Object> submit(@PathVariable Long id,
                                    @RequestBody SubmitInspectionPayload payload) {
    QualityInspection inspection = taskService.submit(id, LoginContext.require().id(),
        payload.resultStatus(), payload.standardVersion());
    return Map.of(
        "taskId", id,
        "inspectionId", inspection.getId(),
        "resultStatus", inspection.getResultStatus(),
        "qualificationSnapshot", inspection.getQualificationSnapshot() == null ? ""
            : inspection.getQualificationSnapshot(),
        "inspectedAt", inspection.getInspectedAt());
  }

  /** 任务列表，可按状态过滤。 */
  @GetMapping(InspectionTaskRoutes.BASE)
  public List<Map<String, Object>> list(@RequestParam(required = false) String status) {
    return taskService.listByStatus(status).stream()
        .map(InspectionTaskDtoFactory::create)
        .toList();
  }

  /** 任务详情。 */
  @GetMapping(InspectionTaskRoutes.BASE + "/{id}")
  public Map<String, Object> detail(@PathVariable Long id) {
    return InspectionTaskDtoFactory.create(taskService.mustGet(id));
  }

  private InspectionType parseType(String raw) {
    InspectionType type = InspectionType.from(raw);
    if (type == null) {
      throw BizException.of("VALIDATION_FAILED", "非法检验类型: " + raw + "，可选 FIRST_INSPECTION/PATROL_INSPECTION/FINAL_INSPECTION");
    }
    return type;
  }

  private String operator() {
    LoginContext.Actor actor = LoginContext.get();
    return actor == null ? "system" : actor.displayName();
  }
}
