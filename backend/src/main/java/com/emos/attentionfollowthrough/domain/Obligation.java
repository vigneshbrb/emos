package com.emos.attentionfollowthrough.domain;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;

public record Obligation(
    ObligationId id,
    ExpectationId expectationId,
    int policyVersion,
    OperationalCaseId caseId,
    Instant sourceResolvedAt,
    Instant deadline,
    ObligationState state,
    Instant breachedAt,
    Instant cancelledAt) {
  public static Obligation pending(OperationalCaseId caseId, Instant resolvedAt, Instant deadline) {
    return new Obligation(
        ObligationId.newId(),
        new ExpectationId("operational-case-disposition"),
        1,
        caseId,
        resolvedAt,
        deadline,
        ObligationState.PENDING,
        null,
        null);
  }
}
