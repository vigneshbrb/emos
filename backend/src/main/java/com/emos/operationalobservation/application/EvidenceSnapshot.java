package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvidenceSnapshot(
    UUID id,
    String snapshotKey,
    OperationalCaseId caseId,
    Instant observedAt,
    String monitorId,
    String jsmSeverity,
    Duration latestDuration,
    RecurrenceCounts recurrence,
    List<String> providerOccurrenceIds,
    List<String> sourceLinks) {
  public EvidenceSnapshot {
    providerOccurrenceIds = List.copyOf(providerOccurrenceIds);
    sourceLinks = List.copyOf(sourceLinks);
  }
}
