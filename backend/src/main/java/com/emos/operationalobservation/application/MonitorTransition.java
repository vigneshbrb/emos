package com.emos.operationalobservation.application;

import java.time.Instant;

public record MonitorTransition(String providerOccurrenceId, String monitorId, Instant occurredAt, State state) {
    public enum State { ALERT, WARNING, OK }
}
