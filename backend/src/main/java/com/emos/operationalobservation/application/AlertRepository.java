package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.Alert;

import java.util.Optional;

public interface AlertRepository {
    Optional<Alert> findBySourceId(String sourceId);

    void insert(Alert alert);

    void update(Alert alert);
}
