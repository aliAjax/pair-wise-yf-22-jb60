package com.generated.qualityTrace.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 产品批次。追溯的核心聚合根：按批号可查到其下的检验任务、检验结论与不良记录。
 */
@TableName("product_batch")
public class ProductBatch {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String batchNo;

  private Long workOrderId;

  private String quantity;

  private String materialLotNo;

  private String producedAt;

  private String batchStatus;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBatchNo() {
    return batchNo;
  }

  public void setBatchNo(String batchNo) {
    this.batchNo = batchNo;
  }

  public Long getWorkOrderId() {
    return workOrderId;
  }

  public void setWorkOrderId(Long workOrderId) {
    this.workOrderId = workOrderId;
  }

  public String getQuantity() {
    return quantity;
  }

  public void setQuantity(String quantity) {
    this.quantity = quantity;
  }

  public String getMaterialLotNo() {
    return materialLotNo;
  }

  public void setMaterialLotNo(String materialLotNo) {
    this.materialLotNo = materialLotNo;
  }

  public String getProducedAt() {
    return producedAt;
  }

  public void setProducedAt(String producedAt) {
    this.producedAt = producedAt;
  }

  public String getBatchStatus() {
    return batchStatus;
  }

  public void setBatchStatus(String batchStatus) {
    this.batchStatus = batchStatus;
  }
}
