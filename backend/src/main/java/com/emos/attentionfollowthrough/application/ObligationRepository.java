package com.emos.attentionfollowthrough.application;

import com.emos.attentionfollowthrough.domain.Obligation;
import com.emos.attentionfollowthrough.domain.ObligationId;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ObligationRepository {
  Optional<Obligation> findForResolution(OperationalCaseId caseId, Instant resolvedAt);

  Obligation insert(Obligation obligation);

  void cancelPending(OperationalCaseId caseId, Instant at);

  List<Obligation> findPendingDue(Instant now);

  boolean markBreached(ObligationId id, Instant at);
}
