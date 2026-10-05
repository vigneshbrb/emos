package com.emos.improvementknowledge.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import java.util.Optional;

public interface RepositoryMappingRepository {
  void confirm(
      MappingId id,
      OperationalCaseId caseId,
      String monitorId,
      RepositoryRef repository,
      String rationale,
      Instant at);

  Optional<RepositoryMapping> findCurrent(String monitorId);
}
