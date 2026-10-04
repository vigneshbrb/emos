package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.operationalobservation.domain.OperationalCaseId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertQueryService {
    List<AlertSummary> findActive();

    Optional<CaseSummary> findByCaseId(OperationalCaseId caseId);

    record AlertSummary(OperationalCaseId caseId, String sourceId, AlertStatus status, String severity,
                        String sourceUrl, Instant updatedAt) {
    }

    record CaseSummary(OperationalCaseId caseId, String sourceId, AlertStatus status, String severity,
                       String sourceUrl, String monitorUrl, String runbook, Instant triggeredAt,
                       Instant resolvedAt, Instant updatedAt) { }
}
