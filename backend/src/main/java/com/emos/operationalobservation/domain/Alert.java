package com.emos.operationalobservation.domain;

import com.emos.operationalobservation.application.JsmAlertSnapshot;
import java.time.Instant;

public final class Alert {
  private final OperationalCaseId caseId;
  private final String sourceId;
  private AlertStatus status;
  private String sourceUrl;
  private String monitorUrl;
  private String runbook;
  private String severity;
  private Instant triggeredAt;
  private Instant resolvedAt;
  private Instant lastSourceUpdatedAt;
  private long version;

  private Alert(OperationalCaseId caseId, JsmAlertSnapshot snapshot, long version) {
    this.caseId = caseId;
    this.sourceId = snapshot.sourceId();
    this.version = version;
    copyFacts(snapshot);
  }

  public static Alert from(JsmAlertSnapshot snapshot) {
    return new Alert(OperationalCaseId.newId(), snapshot, 0);
  }

  public static Alert restore(OperationalCaseId caseId, JsmAlertSnapshot snapshot, long version) {
    return new Alert(caseId, snapshot, version);
  }

  public AlertChange apply(JsmAlertSnapshot snapshot) {
    if (!sourceId.equals(snapshot.sourceId())) {
      throw new IllegalArgumentException("Snapshot belongs to a different alert");
    }
    if (!snapshot.updatedAt().isAfter(lastSourceUpdatedAt)) {
      return new AlertChange(AlertChange.Type.IGNORED, false);
    }

    var previous = status;
    copyFacts(snapshot);
    version++;
    if (previous == AlertStatus.ACTIVE && status == AlertStatus.RESOLVED) {
      return new AlertChange(AlertChange.Type.RESOLVED, true);
    }
    if (previous == AlertStatus.RESOLVED && status == AlertStatus.ACTIVE) {
      return new AlertChange(AlertChange.Type.REOPENED, true);
    }
    return new AlertChange(AlertChange.Type.UPDATED, false);
  }

  private void copyFacts(JsmAlertSnapshot snapshot) {
    status = snapshot.status();
    sourceUrl = snapshot.sourceUrl();
    monitorUrl = snapshot.monitorUrl();
    runbook = snapshot.runbook();
    severity = snapshot.severity();
    triggeredAt = snapshot.triggeredAt();
    resolvedAt = snapshot.status() == AlertStatus.RESOLVED ? snapshot.resolvedAt() : null;
    lastSourceUpdatedAt = snapshot.updatedAt();
  }

  public OperationalCaseId caseId() {
    return caseId;
  }

  public String sourceId() {
    return sourceId;
  }

  public AlertStatus status() {
    return status;
  }

  public String sourceUrl() {
    return sourceUrl;
  }

  public String monitorUrl() {
    return monitorUrl;
  }

  public String runbook() {
    return runbook;
  }

  public String severity() {
    return severity;
  }

  public Instant triggeredAt() {
    return triggeredAt;
  }

  public Instant resolvedAt() {
    return resolvedAt;
  }

  public Instant lastSourceUpdatedAt() {
    return lastSourceUpdatedAt;
  }

  public long version() {
    return version;
  }

  public record AlertChange(Type type, boolean statusChanged) {
    public enum Type {
      IGNORED,
      UPDATED,
      RESOLVED,
      REOPENED
    }
  }
}
