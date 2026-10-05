package com.emos.platform.telemetry;

import java.time.Instant;
import java.util.UUID;

public interface InteractionSessionStore {
  SessionRecord insert(SessionRecord record);

  SessionRecord get(UUID id);

  boolean addHeartbeat(UUID id, Instant at, boolean visibleActive);

  void update(SessionRecord record);
}
