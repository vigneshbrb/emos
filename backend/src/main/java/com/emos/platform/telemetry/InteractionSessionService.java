package com.emos.platform.telemetry;

import java.time.*;
import java.util.UUID;

public class InteractionSessionService {
  private final InteractionSessionStore store;
  private final Duration idleCutoff;

  public InteractionSessionService(InteractionSessionStore s, Duration cutoff) {
    store = s;
    idleCutoff = cutoff;
  }

  public UUID start(UUID caseId, Instant at) {
    var id = UUID.randomUUID();
    store.insert(new SessionRecord(id, caseId, at, at, true, null, 0));
    return id;
  }

  public void heartbeat(UUID id, Instant at, boolean visibleActive) {
    if (!store.addHeartbeat(id, at, visibleActive)) return;
    var current = store.get(id);
    var gap = Duration.between(current.lastHeartbeatAt(), at);
    var add =
        current.lastVisibleActive()
                && visibleActive
                && !gap.isNegative()
                && gap.compareTo(idleCutoff) <= 0
            ? gap.toSeconds()
            : 0;
    store.update(
        new SessionRecord(
            id,
            current.caseId(),
            current.startedAt(),
            at,
            visibleActive,
            current.stoppedAt(),
            current.activeSeconds() + add));
  }

  public void stop(UUID id, Instant at) {
    var current = store.get(id);
    store.update(
        new SessionRecord(
            id,
            current.caseId(),
            current.startedAt(),
            current.lastHeartbeatAt(),
            current.lastVisibleActive(),
            at,
            current.activeSeconds()));
  }
}
