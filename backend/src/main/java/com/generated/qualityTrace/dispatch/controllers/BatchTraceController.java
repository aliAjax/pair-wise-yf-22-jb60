package com.generated.qualityTrace.dispatch.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.generated.qualityTrace.dispatch.middlewares.CallerContext;
import com.generated.qualityTrace.dispatch.routes.DispatchRoutes;
import com.generated.qualityTrace.dispatch.services.BatchTraceService;
import com.generated.qualityTrace.dispatch.types.BatchTraceView;

/** 批次追溯：按批号返回检验员和当时的资质。 */
@RestController
public class BatchTraceController {

  private final BatchTraceService traceService;

  public BatchTraceController(BatchTraceService traceService) {
    this.traceService = traceService;
  }

  @GetMapping(DispatchRoutes.TRACE)
  public BatchTraceView trace(@PathVariable String batchNo) {
    return traceService.trace(batchNo, CallerContext.getCallerEmployeeNo());
  }
}
