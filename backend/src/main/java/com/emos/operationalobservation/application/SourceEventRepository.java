package com.emos.operationalobservation.application;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SourceEventRepository {
    Optional<UUID> insert(String sourceSystem, String sourceId, Instant sourceUpdatedAt, JsonNode rawPayload);

    void markApplied(UUID id);

    void markIgnored(UUID id);
}
