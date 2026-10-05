package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.AlertStatus;
import java.time.Instant;

public record JsmAlertSnapshot(
    String sourceId,
    Instant updatedAt,
    AlertStatus status,
    String sourceUrl,
    String monitorUrl,
    String runbook,
    String severity,
    Instant triggeredAt,
    Instant resolvedAt) {
  public JsmAlertSnapshot {
    if (sourceId == null || sourceId.isBlank())
      throw new IllegalArgumentException("Source ID is required");
    if (updatedAt == null) throw new IllegalArgumentException("Source update time is required");
    if (status == null) throw new IllegalArgumentException("Alert status is required");
    if (status == AlertStatus.RESOLVED && resolvedAt == null) {
      throw new IllegalArgumentException("Resolved alerts require a resolution time");
    }
  }
}
