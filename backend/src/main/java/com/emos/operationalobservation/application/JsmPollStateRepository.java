package com.emos.operationalobservation.application;

import java.time.Instant;

public interface JsmPollStateRepository {
  PollState current();

  void advance(String cursor, Instant updatedSince);

  record PollState(String cursor, Instant updatedSince) {}
}
