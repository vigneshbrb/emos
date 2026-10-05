package com.emos.improvementknowledge.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.*;

public interface FollowUpRepository {
  Optional<FollowUpRecord> find(FollowUpId id);

  Optional<FollowUpRecord> findForCase(OperationalCaseId caseId);

  List<FollowUpRecord> findOpen();

  void save(FollowUpRecord record);
}
