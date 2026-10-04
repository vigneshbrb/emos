package com.emos.operationalobservation.application;

public interface EvidenceRepository {
    EvidenceSnapshot saveIfAbsent(EvidenceSnapshot snapshot);
}
