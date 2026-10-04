package com.generated.qualityTrace.dispatch.constructors;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import com.generated.qualityTrace.dispatch.models.Inspector;
import com.generated.qualityTrace.dispatch.models.InspectorQualification;
import com.generated.qualityTrace.dispatch.types.InspectorQualificationView;
import com.generated.qualityTrace.dispatch.types.InspectorView;

/** 检验员/资质响应对象构造器。 */
@Component
public class InspectorViewFactory {

  public InspectorQualificationView buildQualification(InspectorQualification q, LocalDate today) {
    InspectorQualificationView v = new InspectorQualificationView();
    v.id = q.getId();
    v.qualificationNo = q.getQualificationNo();
    v.inspectionType = q.getInspectionType();
    v.validFrom = q.getValidFrom();
    v.validUntil = q.getValidUntil();
    v.status = q.getStatus();
    v.currentlyValid = q.isValidOn(today);
    return v;
  }

  public InspectorView build(Inspector inspector, List<InspectorQualification> qualifications, LocalDate today) {
    InspectorView v = new InspectorView();
    v.id = inspector.getId();
    v.employeeNo = inspector.getEmployeeNo();
    v.name = inspector.getName();
    v.role = inspector.getRole();
    v.active = inspector.isActive();
    v.qualifications = qualifications.stream()
        .map(q -> buildQualification(q, today))
        .toList();
    return v;
  }
}
