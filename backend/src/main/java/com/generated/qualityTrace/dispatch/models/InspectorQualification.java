package com.generated.qualityTrace.dispatch.models;

import java.time.LocalDate;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 检验员资质：一名检验员可按检验类型持有多条资质。
 * “资质有效” = status='ACTIVE' 且 valid_from <= 今天 <= valid_until。
 */
@Entity
@Table(name = "inspector_qualification")
public class InspectorQualification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "inspector_id", nullable = false)
  private Long inspectorId;

  @Column(name = "qualification_no", nullable = false, unique = true)
  private String qualificationNo;

  @Column(name = "inspection_type", nullable = false)
  private String inspectionType;

  @Column(name = "valid_from", nullable = false)
  private LocalDate validFrom;

  @Column(name = "valid_until", nullable = false)
  private LocalDate validUntil;

  @Column(name = "status", nullable = false)
  private String status = "ACTIVE";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  /** 是否在指定日期有效（状态有效且落在有效期内）。 */
  public boolean isValidOn(LocalDate day) {
    return "ACTIVE".equals(status)
        && !day.isBefore(validFrom)
        && !day.isAfter(validUntil);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getInspectorId() { return inspectorId; }
  public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
  public String getQualificationNo() { return qualificationNo; }
  public void setQualificationNo(String qualificationNo) { this.qualificationNo = qualificationNo; }
  public String getInspectionType() { return inspectionType; }
  public void setInspectionType(String inspectionType) { this.inspectionType = inspectionType; }
  public LocalDate getValidFrom() { return validFrom; }
  public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
  public LocalDate getValidUntil() { return validUntil; }
  public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
