package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EvidenceQueryService {
  Optional<EvidenceSummary> findLatest(OperationalCaseId caseId);

  record EvidenceSummary(
      Instant observedAt,
      String monitorId,
      String severity,
      Long durationSeconds,
      RecurrenceCounts recurrence,
      List<String> providerOccurrenceIds,
      List<String> sourceLinks) {}
}
