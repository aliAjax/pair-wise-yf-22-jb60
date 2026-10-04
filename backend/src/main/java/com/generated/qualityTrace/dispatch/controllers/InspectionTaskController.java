package com.generated.qualityTrace.dispatch.controllers;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorCodes;
import com.generated.qualityTrace.dispatch.constants.DispatchErrorMessages;
import com.generated.qualityTrace.dispatch.exceptions.DispatchBusinessException;
import com.generated.qualityTrace.dispatch.middlewares.CallerContext;
import com.generated.qualityTrace.dispatch.routes.DispatchRoutes;
import com.generated.qualityTrace.dispatch.services.InspectionTaskService;
import com.generated.qualityTrace.dispatch.services.QualificationRecheckService;
import com.generated.qualityTrace.dispatch.types.CreateInspectionTaskPayload;
import com.generated.qualityTrace.dispatch.types.InspectionTaskView;
import com.generated.qualityTrace.dispatch.types.QualificationRecheckView;
import com.generated.qualityTrace.dispatch.types.SubmitInspectionPayload;

/** 检验任务派发/领取/提交入口。 */
@RestController
public class InspectionTaskController {

  private final InspectionTaskService taskService;
  private final QualificationRecheckService recheckService;

  public InspectionTaskController(InspectionTaskService taskService,
                                  QualificationRecheckService recheckService) {
    this.taskService = taskService;
    this.recheckService = recheckService;
  }

  /** 创建检验任务（默认立即按检验类型自动派发给资质有效检验员）。 */
  @PostMapping(DispatchRoutes.TASKS)
  @ResponseStatus(HttpStatus.CREATED)
  public InspectionTaskView create(@Valid @RequestBody CreateInspectionTaskPayload payload) {
    return taskService.create(payload, requireCallerForWrite());
  }

  /** 任务列表，可按状态过滤（如 ?status=DISPATCH_FAILED 找待重试任务）。 */
  @GetMapping(DispatchRoutes.TASKS)
  public List<InspectionTaskView> list(@RequestParam(required = false) String status) {
    return taskService.list(status);
  }

  @GetMapping(DispatchRoutes.TASK_BY_NO)
  public InspectionTaskView get(@PathVariable String taskNo) {
    return taskService.getView(taskNo);
  }

  /** 系统派发（重新/手工派发同一个待派任务）。 */
  @PostMapping(DispatchRoutes.DISPATCH)
  public InspectionTaskView dispatch(@PathVariable String taskNo) {
    return taskService.dispatch(taskNo, requireCallerForWrite());
  }

  /** 派发失败后重试（也可直接调 dispatch，此入口语义更明确并记录重试日志）。 */
  @PostMapping(DispatchRoutes.DISPATCH_RETRY)
  public InspectionTaskView retryDispatch(@PathVariable String taskNo) {
    return taskService.retryDispatch(taskNo, requireCallerForWrite());
  }

  /** 检验员自行领取：需带 X-Inspector-Id；并发时先到先得。 */
  @PostMapping(DispatchRoutes.CLAIM)
  public InspectionTaskView claim(@PathVariable String taskNo) {
    return taskService.claim(taskNo, requireCallerForWrite());
  }

  /** 领取人提交检验结论。 */
  @PostMapping(DispatchRoutes.SUBMIT)
  public InspectionTaskView submit(@PathVariable String taskNo,
                                   @Valid @RequestBody SubmitInspectionPayload payload) {
    return taskService.submit(taskNo, payload.getResultStatus(), payload.getResultNote(),
        requireCallerForWrite());
  }

  /** 资质过期/吊销后，按批号重新确认接单人：未提交退回待派，已提交照旧有效。 */
  @PostMapping(DispatchRoutes.QUALIFICATION_RECHECK)
  public QualificationRecheckView recheck(@PathVariable String batchNo) {
    return recheckService.recheck(batchNo, requireCallerForWrite());
  }

  private String requireCallerForWrite() {
    String caller = CallerContext.getCallerEmployeeNo();
    if (caller == null || caller.isBlank()) {
      throw DispatchBusinessException.badRequest(
          DispatchErrorCodes.CALLER_REQUIRED, DispatchErrorMessages.CALLER_REQUIRED);
    }
    return caller;
  }
}
