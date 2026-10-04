package com.generated.qualityTrace.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 格式化工具。混合日期、日志模板、消息模板格式化逻辑，被 service/controller 共同依赖。
 *
 * <p>日志与消息模板统一使用 {0}、{1} … 占位符，由本类按顺序替换。</p>
 */
public final class Formatters {

  private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private Formatters() {
  }

  /** 旧的审计对象格式化，保留兼容。 */
  public static String audit(String type, long id) {
    return type + "#" + id;
  }

  /** 当前时间 yyyy-MM-dd HH:mm:ss。 */
  public static String now() {
    return LocalDateTime.now().format(DT);
  }

  /**
   * 把模板中的 {0}、{1} … 按顺序替换为参数。
   */
  public static String format(String template, Object... args) {
    if (template == null) {
      return "";
    }
    String result = template;
    for (int i = 0; i < args.length; i++) {
      String value = args[i] == null ? "" : args[i].toString();
      result = result.replace("{" + i + "}", value);
    }
    return result;
  }

  /**
   * 渲染日志模板：把动作模板与参数拼成一条可读日志。
   * 用法：{@code log.info(Formatters.audit(LogTemplates.TASK_CLAIM, taskNo, inspector, type))}
   */
  public static String audit(String template, Object... args) {
    return format(template, args);
  }
}
