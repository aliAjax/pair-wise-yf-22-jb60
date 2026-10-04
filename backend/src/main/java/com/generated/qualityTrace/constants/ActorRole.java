package com.generated.qualityTrace.constants;

/**
 * 系统角色（RBAC）。
 *
 * <ul>
 *   <li>INSPECTOR：质检员 —— 领取任务、提交检验结论。</li>
 *   <li>SUPERVISOR：产线主管 —— 创建任务、派发/重试、退回任务。</li>
 *   <li>MANAGER：质量经理 —— 授予/吊销检验员资质、重新确认接单人。</li>
 *   <li>AUDITOR：审计员 —— 只读追溯。</li>
 * </ul>
 */
public enum ActorRole {
  INSPECTOR("质检员"),
  SUPERVISOR("产线主管"),
  MANAGER("质量经理"),
  AUDITOR("审计员");

  private final String label;

  ActorRole(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static ActorRole from(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String normalized = raw.trim().toUpperCase();
    for (ActorRole r : values()) {
      if (r.name().equals(normalized)) {
        return r;
      }
    }
    return null;
  }
}
