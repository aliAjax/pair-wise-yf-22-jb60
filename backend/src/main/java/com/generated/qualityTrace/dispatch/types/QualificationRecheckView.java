package com.generated.qualityTrace.dispatch.types;

import java.util.List;

/** 资质复检结果：还没提交的退回待派，已提交的保持有效。 */
public class QualificationRecheckView {
  public String batchNo;
  public int claimedRechecked;
  public int returnedToPending;
  public int submittedKeptValid;
  /** 复检后资质仍有效、保持接单的任务。 */
  public List<String> confirmedTaskNos;
  /** 资质失效被退回待派的任务（可重新派发/领取）。 */
  public List<String> returnedTaskNos;
  /** 已提交、结论照旧有效的任务。 */
  public List<String> submittedTaskNos;
}
