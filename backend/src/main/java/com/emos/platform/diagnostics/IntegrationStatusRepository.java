package com.emos.platform.diagnostics;

import java.time.Instant;
import java.util.List;

public interface IntegrationStatusRepository {
  void recordSuccess(String provider, Instant at);

  void recordFailure(String provider, Instant at, String safeMessage);

  List<IntegrationStatus> findAll();
}
