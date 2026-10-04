package com.emos.operationalobservation.application;

import java.util.List;

public record MonitorEvidence(String monitorId, String monitorUrl, List<MonitorTransition> transitions) {
    public MonitorEvidence {
        transitions = List.copyOf(transitions);
    }
}
