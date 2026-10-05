package com.emos.operationalobservation.application;

import java.util.List;

public record JsmAlertPage(List<JsmAlertRecord> alerts, String nextCursor) {
  public JsmAlertPage {
    alerts = List.copyOf(alerts);
  }
}
