package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.middlewares.RequireRole;
import com.generated.qualityTrace.routes.TraceRoutes;
import com.generated.qualityTrace.services.TraceService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 追溯控制器：按批号返回批次全链路（任务 + 检验结论 + 检验员与当时资质）。
 * 质检员/主管/经理/审计员均可查询。
 */
@RestController
public class TraceController {

  private final TraceService traceService;

  public TraceController(TraceService traceService) {
    this.traceService = traceService;
  }

  @GetMapping(TraceRoutes.BASE + "/{batchNo}")
  @RequireRole({ActorRole.INSPECTOR, ActorRole.SUPERVISOR, ActorRole.MANAGER, ActorRole.AUDITOR})
  public Map<String, Object> trace(@PathVariable String batchNo) {
    LoginContext.Actor actor = LoginContext.get();
    return traceService.traceByBatchNo(batchNo,
        actor == null ? "anonymous" : actor.displayName());
  }
}
