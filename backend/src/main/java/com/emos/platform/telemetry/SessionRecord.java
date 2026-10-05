package com.emos.platform.telemetry;

import java.time.Instant;
import java.util.UUID;

public record SessionRecord(
    UUID id,
    UUID caseId,
    Instant startedAt,
    Instant lastHeartbeatAt,
    boolean lastVisibleActive,
    Instant stoppedAt,
    long activeSeconds) {}
