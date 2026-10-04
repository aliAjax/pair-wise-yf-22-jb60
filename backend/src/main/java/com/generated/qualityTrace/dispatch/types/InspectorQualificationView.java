package com.generated.qualityTrace.dispatch.types;

import java.time.LocalDate;

/** 检验员资质视图。 */
public class InspectorQualificationView {
  public Long id;
  public String qualificationNo;
  public String inspectionType;
  public LocalDate validFrom;
  public LocalDate validUntil;
  public String status;
  /** 按当天计算出的有效性：过期/吊销为 false。 */
  public boolean currentlyValid;
}
