package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.domain.OperationalCaseId;
import com.emos.recommendations.application.RecommendationService;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OperationalCaseSignalSource implements RepositorySignalSource {
  private final EvidenceQueryService evidence;
  private final ObjectProvider<RecommendationService> recommendations;

  public OperationalCaseSignalSource(
      EvidenceQueryService evidence, ObjectProvider<RecommendationService> recommendations) {
    this.evidence = evidence;
    this.recommendations = recommendations;
  }

  public RepositoryCandidateService.CaseSignals signalsFor(OperationalCaseId id) {
    var e =
        evidence
            .findLatest(id)
            .orElseThrow(() -> new IllegalArgumentException("Evidence is required"));
    var deterministic = new ArrayList<String>();
    deterministic.add(e.monitorId());
    if (e.severity() != null) deterministic.add(e.severity());
    var provider = recommendations.getIfAvailable();
    var hints =
        provider == null
            ? List.<String>of()
            : provider
                .findFor(id)
                .filter(r -> r.result() != null)
                .map(r -> r.result().repositorySearchTerms())
                .orElse(List.of());
    return new RepositoryCandidateService.CaseSignals(e.monitorId(), deterministic, hints);
  }
}
