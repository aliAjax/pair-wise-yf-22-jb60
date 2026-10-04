package com.generated.qualityTrace.dispatch.controllers;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.generated.qualityTrace.dispatch.constructors.InspectorViewFactory;
import com.generated.qualityTrace.dispatch.repositories.InspectorRepository;
import com.generated.qualityTrace.dispatch.repositories.InspectorQualificationRepository;
import com.generated.qualityTrace.dispatch.routes.DispatchRoutes;
import com.generated.qualityTrace.dispatch.types.InspectorView;

/** 只读：查看检验员及其资质（含是否在有效期内）。 */
@RestController
@RequestMapping(DispatchRoutes.INSPECTORS)
public class InspectorController {

  private final InspectorRepository inspectorRepository;
  private final InspectorQualificationRepository qualificationRepository;
  private final InspectorViewFactory viewFactory;

  public InspectorController(InspectorRepository inspectorRepository,
                             InspectorQualificationRepository qualificationRepository,
                             InspectorViewFactory viewFactory) {
    this.inspectorRepository = inspectorRepository;
    this.qualificationRepository = qualificationRepository;
    this.viewFactory = viewFactory;
  }

  @GetMapping
  public List<InspectorView> list() {
    LocalDate today = LocalDate.now();
    return inspectorRepository.findByActiveTrueOrderByIdAsc().stream()
        .map(i -> viewFactory.build(
            i, qualificationRepository.findByInspectorId(i.getId()), today))
        .toList();
  }
}
