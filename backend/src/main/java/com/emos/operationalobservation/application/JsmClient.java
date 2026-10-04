package com.emos.operationalobservation.application;

import java.time.Instant;

public interface JsmClient {
    JsmAlertPage fetchAlerts(Instant updatedSince, String cursor);
}
