package com.emos.platform.audit;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditEntry(
        UUID id,
        String subjectType,
        UUID subjectId,
        String eventType,
        ActorType actorType,
        Instant occurredAt,
        JsonNode details,
        List<UUID> evidenceIds
) {
    public AuditEntry {
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }

    public enum ActorType {
        SOURCE,
        AI,
        MANAGER,
        SYSTEM
    }
}
