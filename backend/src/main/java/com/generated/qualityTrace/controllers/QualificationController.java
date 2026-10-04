package com.generated.qualityTrace.controllers;

import com.generated.qualityTrace.constants.ActorRole;
import com.generated.qualityTrace.constants.InspectionType;
import com.generated.qualityTrace.constructors.InspectorQualificationDtoFactory;
import com.generated.qualityTrace.context.LoginContext;
import com.generated.qualityTrace.exceptions.BizException;
import com.generated.qualityTrace.middlewares.RequireRole;
import com.generated.qualityTrace.models.InspectorQualification;
import com.generated.qualityTrace.routes.QualificationRoutes;
import com.generated.qualityTrace.services.QualificationService;
import com.generated.qualityTrace.types.GrantQualificationPayload;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 检验员资质控制器。授予/吊销/到期重新确认面向质量经理，查询按角色开放。
 */
@RestController
public class QualificationController {

  private final QualificationService qualificationService;

  public QualificationController(QualificationService qualificationService) {
    this.qualificationService = qualificationService;
  }

  /** 授予（或重新授予）检验员资质。 */
  @PostMapping(QualificationRoutes.BASE)
  @RequireRole(ActorRole.MANAGER)
  public Map<String, Object> grant(@RequestBody GrantQualificationPayload payload) {
    InspectionType type = parseType(payload.inspectionType());
    InspectorQualification q = qualificationService.grant(payload.inspectorId(), type,
        payload.qualifiedFrom(), payload.qualifiedUntil(), operator());
    return InspectorQualificationDtoFactory.create(q);
  }

  /** 吊销资质：该检验员已接单未提交的任务退回待派，已提交结论不受影响。 */
  @PostMapping(QualificationRoutes.BASE + QualificationRoutes.REVOKE)
  @RequireRole(ActorRole.MANAGER)
  public Map<String, Object> revoke(@PathVariable Long id) {
    InspectorQualification q = qualificationService.revoke(id, operator());
    return InspectorQualificationDtoFactory.create(q);
  }

  /** 手动触发资质到期重新确认（批处理也会每日自动执行）。 */
  @PostMapping(QualificationRoutes.BASE + QualificationRoutes.REVALIDATE)
  @RequireRole(ActorRole.MANAGER)
  public Map<String, Object> revalidate() {
    int reverted = qualificationService.revalidateExpired();
    return Map.of("revertedTasks", reverted, "operator", operator());
  }

  /** 查询某检验员的资质列表。 */
  @GetMapping(QualificationRoutes.BASE)
  public List<Map<String, Object>> listByInspector(@RequestParam Long inspectorId) {
    return qualificationService.listByInspector(inspectorId).stream()
        .map(InspectorQualificationDtoFactory::create)
        .toList();
  }

  private InspectionType parseType(String raw) {
    InspectionType type = InspectionType.from(raw);
    if (type == null) {
      throw BizException.of("VALIDATION_FAILED", "非法检验类型: " + raw);
    }
    return type;
  }

  private String operator() {
    LoginContext.Actor actor = LoginContext.get();
    return actor == null ? "system" : actor.displayName();
  }
}
