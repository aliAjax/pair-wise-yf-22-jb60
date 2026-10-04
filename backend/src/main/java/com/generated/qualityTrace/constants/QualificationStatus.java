package com.generated.qualityTrace.constants;

/**
 * 检验员资质状态。
 *
 * <ul>
 *   <li>VALID：在有效期内，可接单。</li>
 *   <li>EXPIRED：已过有效期，系统批处理会把已接单未提交的任务退回待派。</li>
 *   <li>REVOKED：被管理员手动吊销，不可恢复接单。</li>
 * </ul>
 */
public enum QualificationStatus {
  VALID("有效"),
  EXPIRED("已过期"),
  REVOKED("已吊销");

  private final String label;

  QualificationStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** 资质是否仍可用于接单：状态有效且未过期（过期判断另见服务层日期校验）。 */
  public boolean isUsable() {
    return this == VALID;
  }
}
