package com.emos.attentionfollowthrough.application;

import com.emos.attentionfollowthrough.domain.ObligationId;
import com.emos.attentionfollowthrough.domain.ObligationState;
import com.emos.operationalobservation.domain.OperationalCaseId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AttentionQueryService {
    List<AttentionSummary> findOpen();
    List<PendingObligationSummary> findPending();

    record AttentionSummary(UUID id, ObligationId obligationId, OperationalCaseId caseId,
                            String reason, Instant openedAt) { }
    record PendingObligationSummary(ObligationId id, OperationalCaseId caseId, ObligationState state,
                                    Instant deadline) { }
}
