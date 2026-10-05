package com.emos.attentionfollowthrough.domain;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import java.util.UUID;

public record AttentionItem(
    UUID id,
    ObligationId obligationId,
    OperationalCaseId caseId,
    String reason,
    State state,
    Instant openedAt,
    Instant resolvedAt) {
  public enum State {
    OPEN,
    RESOLVED
  }

  public static AttentionItem missingDisposition(Obligation obligation, Instant openedAt) {
    return new AttentionItem(
        UUID.randomUUID(),
        obligation.id(),
        obligation.caseId(),
        "Improvement decision missing",
        State.OPEN,
        openedAt,
        null);
  }
}
