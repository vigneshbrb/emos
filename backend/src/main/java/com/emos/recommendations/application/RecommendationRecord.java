package com.emos.recommendations.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import java.util.UUID;

public record RecommendationRecord(
    UUID id,
    String deduplicationKey,
    OperationalCaseId caseId,
    String evidenceVersion,
    String promptVersion,
    RecommendationStatus status,
    RecommendationResult result,
    Instant requestedAt,
    Instant generatedAt,
    String failureMessage) {
  RecommendationRecord withReady(RecommendationResult result, Instant at) {
    return new RecommendationRecord(
        id,
        deduplicationKey,
        caseId,
        evidenceVersion,
        promptVersion,
        RecommendationStatus.READY,
        result,
        requestedAt,
        at,
        null);
  }

  RecommendationRecord withFailed(String message, Instant at) {
    return new RecommendationRecord(
        id,
        deduplicationKey,
        caseId,
        evidenceVersion,
        promptVersion,
        RecommendationStatus.FAILED,
        null,
        requestedAt,
        at,
        message);
  }

  RecommendationRecord asStale() {
    return new RecommendationRecord(
        id,
        deduplicationKey,
        caseId,
        evidenceVersion,
        promptVersion,
        RecommendationStatus.STALE,
        result,
        requestedAt,
        generatedAt,
        failureMessage);
  }
}
