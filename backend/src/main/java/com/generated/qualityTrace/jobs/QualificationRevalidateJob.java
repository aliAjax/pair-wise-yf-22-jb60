package com.generated.qualityTrace.jobs;

import com.generated.qualityTrace.services.QualificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 资质到期批处理：每日凌晨扫描已过有效期的资质，把已接单未提交的任务退回待派。
 * 已提交结论照旧有效。也可通过 POST /api/qualifications/revalidate 手动触发。
 */
@Component
public class QualificationRevalidateJob {

  private static final Logger log = LoggerFactory.getLogger(QualificationRevalidateJob.class);

  private final QualificationService qualificationService;

  public QualificationRevalidateJob(QualificationService qualificationService) {
    this.qualificationService = qualificationService;
  }

  /** 每天凌晨 03:15 执行。 */
  @Scheduled(cron = "0 15 3 * * ?")
  public void dailyRevalidate() {
    int reverted = qualificationService.revalidateExpired();
    log.info("scheduled qualification revalidate finished, reverted tasks={}", reverted);
  }
}
