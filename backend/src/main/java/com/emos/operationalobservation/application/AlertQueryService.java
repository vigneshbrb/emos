package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.operationalobservation.domain.OperationalCaseId;

import java.time.Instant;
import java.util.List;

public interface AlertQueryService {
    List<AlertSummary> findActive();

    record AlertSummary(OperationalCaseId caseId, String sourceId, AlertStatus status, String severity,
                        String sourceUrl, Instant updatedAt) {
    }
}
