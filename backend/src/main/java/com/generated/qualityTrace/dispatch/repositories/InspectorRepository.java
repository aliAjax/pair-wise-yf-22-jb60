package com.generated.qualityTrace.dispatch.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.generated.qualityTrace.dispatch.models.Inspector;

public interface InspectorRepository extends JpaRepository<Inspector, Long> {
  Optional<Inspector> findByEmployeeNo(String employeeNo);
  List<Inspector> findByActiveTrueOrderByIdAsc();
}
