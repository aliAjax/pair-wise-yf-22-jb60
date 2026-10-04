package com.generated.qualityTrace.constants;

/**
 * 检验类型。任务按检验类型派发给持有对应有效资质的检验员。
 */
public enum InspectionType {
  FIRST_INSPECTION("首检"),
  PATROL_INSPECTION("巡检"),
  FINAL_INSPECTION("终检");

  private final String label;

  InspectionType(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** 兼容按名称（大小写不敏感）解析，非法值返回 null。 */
  public static InspectionType from(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String normalized = raw.trim().toUpperCase().replace('-', '_');
    for (InspectionType t : values()) {
      if (t.name().equals(normalized)) {
        return t;
      }
    }
    return null;
  }
}
