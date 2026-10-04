package com.generated.qualityTrace.dispatch.routes;

/** 派发域路由常量。 */
public final class DispatchRoutes {
  private DispatchRoutes() {}

  public static final String BASE = "/api/dispatch";
  public static final String TASKS = BASE + "/tasks";
  public static final String TASK_BY_NO = BASE + "/tasks/{taskNo}";
  public static final String DISPATCH = BASE + "/tasks/{taskNo}/dispatch";
  public static final String DISPATCH_RETRY = BASE + "/tasks/{taskNo}/retry-dispatch";
  public static final String CLAIM = BASE + "/tasks/{taskNo}/claim";
  public static final String SUBMIT = BASE + "/tasks/{taskNo}/submit";
  public static final String QUALIFICATION_RECHECK = BASE + "/batches/{batchNo}/recheck-qualifications";
  public static final String TRACE = "/api/trace/{batchNo}";
  public static final String INSPECTORS = BASE + "/inspectors";
}
