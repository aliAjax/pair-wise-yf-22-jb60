package com.generated.qualityTrace.dispatch.middlewares;

/**
 * 调用方身份上下文：由 CallerInterceptor 从 X-Inspector-Id 请求头解析后放入线程变量，
 * controller/service 通过它识别“是谁在领取/提交”，用于越权拒绝。
 */
public final class CallerContext {

  private static final ThreadLocal<String> CALLER_EMPLOYEE_NO = new ThreadLocal<>();

  private CallerContext() {}

  public static void setCallerEmployeeNo(String employeeNo) {
    CALLER_EMPLOYEE_NO.set(employeeNo);
  }

  public static String getCallerEmployeeNo() {
    return CALLER_EMPLOYEE_NO.get();
  }

  public static void clear() {
    CALLER_EMPLOYEE_NO.remove();
  }
}
