package com.emos.improvementknowledge.application;

import com.emos.improvementknowledge.domain.JiraDraft;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.*;
import java.util.*;

public interface ImprovementRepository {
  Optional<JiraDraft> findDraft(UUID id, OperationalCaseId caseId);

  Optional<JiraDraft> findDraft(OperationalCaseId caseId);

  JiraDraft saveDraft(JiraDraft draft);

  boolean isConfirmed(OperationalCaseId caseId, String repositoryId);

  Optional<JiraIssueRef> findAttempt(UUID attemptId);

  void record(
      UUID attemptId,
      OperationalCaseId caseId,
      String correlation,
      JiraIssueRef issue,
      LocalDate reviewDate,
      Instant at);
}
