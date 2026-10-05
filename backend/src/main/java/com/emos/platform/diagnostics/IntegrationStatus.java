package com.emos.platform.diagnostics;

import java.time.Instant;

public record IntegrationStatus(
    String provider, Instant lastSuccessAt, Instant lastFailureAt, String safeMessage) {}
