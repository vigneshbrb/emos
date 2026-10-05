package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CurrentRepositoryMappingLookup implements RepositoryMappingLookup {
  private final EvidenceQueryService evidence;
  private final RepositoryMappingService mappings;

  public CurrentRepositoryMappingLookup(
      EvidenceQueryService evidence, RepositoryMappingService mappings) {
    this.evidence = evidence;
    this.mappings = mappings;
  }

  public Optional<RepositoryMapping> findFor(OperationalCaseId id) {
    return evidence.findLatest(id).flatMap(e -> mappings.findCurrent(e.monitorId()));
  }
}
