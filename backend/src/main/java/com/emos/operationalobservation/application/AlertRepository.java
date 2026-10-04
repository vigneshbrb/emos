package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.Alert;
import com.emos.operationalobservation.domain.OperationalCaseId;

import java.util.Optional;

public interface AlertRepository {
    Optional<Alert> findBySourceId(String sourceId);

    Optional<Alert> findByCaseId(OperationalCaseId caseId);

    void insert(Alert alert);

    void update(Alert alert);
}
