package com.generated.qualityTrace.services;

import com.generated.qualityTrace.constants.ErrorCodes;
import com.generated.qualityTrace.constants.ErrorMessages;
import com.generated.qualityTrace.constants.LogTemplates;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.models.InspectionTask;
import com.generated.qualityTrace.models.ProductBatch;
import com.generated.qualityTrace.models.QualityInspection;
import com.generated.qualityTrace.repositories.InspectionTaskMapper;
import com.generated.qualityTrace.repositories.ProductBatchMapper;
import com.generated.qualityTrace.repositories.QualityInspectionMapper;
import com.generated.qualityTrace.utils.Formatters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 追溯服务：按批号聚合批次 → 检验任务 → 检验结论，
 * 结论中固化了检验员与当时的资质快照，满足「按批号追溯返回检验员和当时的资质」。
 */
@Service
public class TraceService {

  private static final Logger log = LoggerFactory.getLogger(TraceService.class);

  private final ProductBatchMapper batchMapper;
  private final InspectionTaskMapper taskMapper;
  private final QualityInspectionMapper inspectionMapper;

  public TraceService(ProductBatchMapper batchMapper,
                      InspectionTaskMapper taskMapper,
                      QualityInspectionMapper inspectionMapper) {
    this.batchMapper = batchMapper;
    this.taskMapper = taskMapper;
    this.inspectionMapper = inspectionMapper;
  }

  /**
   * 按批号追溯。
   */
  public Map<String, Object> traceByBatchNo(String batchNo, String operator) {
    ProductBatch batch = batchMapper.findByBatchNo(batchNo);
    if (batch == null) {
      throw BizException.of(ErrorCodes.BATCH_NOT_FOUND,
          Formatters.format(ErrorMessages.BATCH_NOT_FOUND, batchNo));
    }

    List<InspectionTask> tasks = taskMapper.findByBatch(batch.getId());
    List<Map<String, Object>> taskViews = new ArrayList<>();
    for (InspectionTask task : tasks) {
      Map<String, Object> view = new LinkedHashMap<>();
      view.put("taskId", task.getId());
      view.put("taskNo", task.getTaskNo());
      view.put("inspectionType", task.getInspectionType());
      view.put("status", task.getStatus());
      view.put("assigneeId", task.getAssigneeId());
      view.put("dispatchedAt", task.getDispatchedAt());
      view.put("claimedAt", task.getClaimedAt());
      view.put("submittedAt", task.getSubmittedAt());

      QualityInspection inspection = inspectionMapper.findByTask(task.getId());
      if (inspection != null) {
        Map<String, Object> conclusion = new LinkedHashMap<>();
        conclusion.put("inspectionId", inspection.getId());
        conclusion.put("inspectorId", inspection.getInspectorId());
        conclusion.put("inspectorName", inspection.getInspectorName());
        conclusion.put("inspectionType", inspection.getInspectionType());
        conclusion.put("resultStatus", inspection.getResultStatus());
        conclusion.put("standardVersion", inspection.getStandardVersion());
        conclusion.put("qualificationId", inspection.getQualificationId());
        conclusion.put("qualificationSnapshot", inspection.getQualificationSnapshot());
        conclusion.put("inspectedAt", inspection.getInspectedAt());
        view.put("conclusion", conclusion);
      }
      taskViews.add(view);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("batchNo", batch.getBatchNo());
    result.put("batchId", batch.getId());
    result.put("product", Map.of(
        "quantity", batch.getQuantity() == null ? "" : batch.getQuantity(),
        "materialLotNo", batch.getMaterialLotNo() == null ? "" : batch.getMaterialLotNo(),
        "producedAt", batch.getProducedAt() == null ? "" : batch.getProducedAt(),
        "batchStatus", batch.getBatchStatus() == null ? "" : batch.getBatchStatus()));
    result.put("taskCount", taskViews.size());
    result.put("tasks", taskViews);

    log.info(Formatters.audit(LogTemplates.TRACE_QUERY, batchNo, operator));
    return result;
  }
}
