package com.emos.operationalobservation.application;

import java.time.Instant;

public interface DatadogClient {
  MonitorEvidence loadEvidence(String monitorId, Instant asOf);
}
