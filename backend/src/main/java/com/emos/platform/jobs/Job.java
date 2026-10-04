package com.emos.platform.jobs;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record Job(
        UUID id,
        String type,
        String deduplicationKey,
        JsonNode payload,
        State state,
        Instant availableAt,
        Instant leaseUntil,
        int attempts,
        String lastError
) {
    public enum State {
        READY,
        RUNNING,
        SUCCEEDED
    }
}
