package com.emos.platform.jobs;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record Job(
    UUID id,
    String type,
    String deduplicationKey,
    JsonNode payload,
    State state,
    Instant availableAt,
    Instant leaseUntil,
    int attempts,
    String lastError) {
  public enum State {
    READY,
    RUNNING,
    SUCCEEDED
  }
}
