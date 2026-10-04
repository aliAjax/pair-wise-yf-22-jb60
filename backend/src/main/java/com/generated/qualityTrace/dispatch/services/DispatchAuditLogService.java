package com.generated.qualityTrace.dispatch.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.generated.qualityTrace.dispatch.models.DispatchAuditLog;
import com.generated.qualityTrace.dispatch.repositories.DispatchAuditLogRepository;

/** 操作日志 + 追溯事件日志：所有派发写操作同时落应用日志与 audit_log 表。 */
@Service
public class DispatchAuditLogService {

  private static final Logger log = LoggerFactory.getLogger(DispatchAuditLogService.class);

  private final DispatchAuditLogRepository auditLogRepository;

  public DispatchAuditLogService(DispatchAuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public void record(String actor, String action, String targetType, String targetId, String detail) {
    log.info("audit actor={} action={} target={}/{} detail={}", actor, action, targetType, targetId, detail);
    auditLogRepository.save(new DispatchAuditLog(actor, action, targetType, targetId, detail));
  }
}
