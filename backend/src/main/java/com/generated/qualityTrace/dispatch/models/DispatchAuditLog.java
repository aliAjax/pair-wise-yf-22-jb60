package com.generated.qualityTrace.dispatch.models;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 操作/追溯事件审计日志。 */
@Entity
@Table(name = "audit_log")
public class DispatchAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "actor")
  private String actor;

  @Column(name = "action")
  private String action;

  @Column(name = "target_type")
  private String targetType;

  @Column(name = "target_id")
  private String targetId;

  @Column(name = "detail")
  private String detail;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public DispatchAuditLog() {}

  public DispatchAuditLog(String actor, String action, String targetType, String targetId, String detail) {
    this.actor = actor;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.detail = detail;
    this.createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getActor() { return actor; }
  public void setActor(String actor) { this.actor = actor; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getTargetType() { return targetType; }
  public void setTargetType(String targetType) { this.targetType = targetType; }
  public String getTargetId() { return targetId; }
  public void setTargetId(String targetId) { this.targetId = targetId; }
  public String getDetail() { return detail; }
  public void setDetail(String detail) { this.detail = detail; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
