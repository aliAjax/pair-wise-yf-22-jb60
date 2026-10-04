package com.generated.qualityTrace.dispatch.types;

import java.util.List;

/** 按批号追溯返回：批次下全部检验任务 + 各自检验员和当时资质。 */
public class BatchTraceView {
  public String batchNo;
  public int inspectionCount;
  public int submittedCount;
  public List<TraceInspectionView> inspections;
}
