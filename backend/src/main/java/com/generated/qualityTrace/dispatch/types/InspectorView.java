package com.generated.qualityTrace.dispatch.types;

import java.util.List;

/** 检验员视图：档案 + 当前资质清单。 */
public class InspectorView {
  public Long id;
  public String employeeNo;
  public String name;
  public String role;
  public boolean active;
  public List<InspectorQualificationView> qualifications;
}
