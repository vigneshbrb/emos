package com.emos.platform.jobs;

import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobRepository {
    Job enqueue(String type, String deduplicationKey, JsonNode payload, Instant availableAt);

    List<Job> claimAvailable(int limit, Instant now, Duration leaseDuration);

    void markSucceeded(UUID id, Instant completedAt);

    void markFailed(UUID id, Instant availableAt, String safeMessage);

    List<Job> findAll();
}
