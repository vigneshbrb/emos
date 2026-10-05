package com.emos.improvementknowledge.application;

import com.emos.improvementknowledge.domain.JiraDraft;
import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.domain.OperationalCaseId;
import com.emos.recommendations.application.RecommendationService;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JiraDraftService {
  private final ImprovementRepository drafts;
  private final RepositoryMappingLookup mappings;
  private final EvidenceQueryService evidence;
  private final ObjectProvider<RecommendationService> recommendations;
  private final Clock clock;

  public JiraDraftService(
      ImprovementRepository drafts,
      RepositoryMappingLookup mappings,
      EvidenceQueryService evidence,
      ObjectProvider<RecommendationService> recommendations,
      Clock clock) {
    this.drafts = drafts;
    this.mappings = mappings;
    this.evidence = evidence;
    this.recommendations = recommendations;
    this.clock = clock;
  }

  @Transactional
  public JiraDraft prepare(OperationalCaseId caseId) {
    return drafts
        .findDraft(caseId)
        .orElseGet(
            () -> {
              var mapping =
                  mappings
                      .findFor(caseId)
                      .filter(RepositoryMapping::accessible)
                      .orElseThrow(
                          () -> new ImprovementValidationException("Repository must be confirmed"));
              var observed =
                  evidence
                      .findLatest(caseId)
                      .orElseThrow(
                          () -> new ImprovementValidationException("Evidence is required"));
              var provider = recommendations.getIfAvailable();
              var result =
                  provider == null
                      ? null
                      : provider
                          .findFor(caseId)
                          .filter(r -> r.result() != null)
                          .map(r -> r.result())
                          .orElse(null);
              var title = result == null ? "Investigate " + observed.monitorId() : result.summary();
              var direction =
                  result == null
                      ? "Investigate the evidence and propose a preventive change"
                      : result.proposedImprovement();
              var summary =
                  "severity="
                      + observed.severity()
                      + ", durationSeconds="
                      + observed.durationSeconds()
                      + ", recurrence24h="
                      + observed.recurrence().last24Hours();
              return drafts.saveDraft(
                  new JiraDraft(
                      UUID.randomUUID(),
                      caseId,
                      title,
                      summary,
                      summary,
                      direction,
                      "Define measurable prevention or mitigation",
                      mapping.repositoryId(),
                      LocalDate.now(clock).plusDays(7),
                      false,
                      clock.instant()));
            });
  }

  @Transactional
  public JiraDraft update(JiraDraft draft) {
    if (draft.reviewDate() == null)
      throw new ImprovementValidationException("Review date is required");
    return drafts.saveDraft(
        new JiraDraft(
            draft.id(),
            draft.caseId(),
            required(draft.title(), "Title"),
            required(draft.problemStatement(), "Problem statement"),
            draft.evidenceSummary(),
            required(draft.proposedDirection(), "Proposed direction"),
            required(draft.acceptanceIntent(), "Acceptance intent"),
            draft.repositoryId(),
            draft.reviewDate(),
            draft.approved(),
            clock.instant()));
  }

  private static String required(String value, String field) {
    if (value == null || value.isBlank())
      throw new ImprovementValidationException(field + " is required");
    return value.strip();
  }
}
