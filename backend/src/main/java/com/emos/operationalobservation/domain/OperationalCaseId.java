package com.emos.operationalobservation.domain;

import java.util.UUID;

public record OperationalCaseId(UUID value) {
    public OperationalCaseId {
        if (value == null) throw new IllegalArgumentException("Operational case ID is required");
    }

    public static OperationalCaseId newId() {
        return new OperationalCaseId(UUID.randomUUID());
    }
}
