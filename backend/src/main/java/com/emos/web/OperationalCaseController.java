package com.emos.web;

import com.emos.operationalobservation.application.AlertQueryService;
import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.operationalobservation.domain.OperationalCaseId;
import com.emos.platform.audit.AuditQueryService;
import com.emos.recommendations.application.RecommendationRecord;
import com.emos.recommendations.application.RecommendationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OperationalCaseController {
  private final AlertQueryService alerts;
  private final EvidenceQueryService evidence;
  private final AuditQueryService audit;
  private final Clock clock;
  private final ObjectProvider<RecommendationService> recommendations;

  public OperationalCaseController(
      AlertQueryService alerts,
      EvidenceQueryService evidence,
      AuditQueryService audit,
      Clock clock,
      ObjectProvider<RecommendationService> recommendations) {
    this.alerts = alerts;
    this.evidence = evidence;
    this.audit = audit;
    this.clock = clock;
    this.recommendations = recommendations;
  }

  @GetMapping("/{caseId}")
  CaseDetail get(@PathVariable UUID caseId) {
    var id = new OperationalCaseId(caseId);
    var alert = alerts.findByCaseId(id).orElseThrow(() -> new CaseNotFoundException(caseId));
    var observed = evidence.findLatest(id);
    var evidenceDto =
        observed
            .map(
                value ->
                    new EvidenceDto(
                        value.monitorId(),
                        value.durationSeconds(),
                        value.severity(),
                        Map.of(
                            "24h",
                            value.recurrence().last24Hours(),
                            "7d",
                            value.recurrence().last7Days(),
                            "30d",
                            value.recurrence().last30Days()),
                        new Freshness(
                            value.observedAt(),
                            value
                                .observedAt()
                                .isBefore(clock.instant().minus(Duration.ofMinutes(15))))))
            .orElse(null);
    var links = new LinkedHashSet<String>();
    add(links, alert.sourceUrl());
    add(links, alert.monitorUrl());
    observed.ifPresent(value -> links.addAll(value.sourceLinks()));
    var timeline =
        audit.findForSubject("ALERT", caseId).stream()
            .map(
                entry ->
                    new TimelineEntry(
                        entry.eventType(), entry.actorType().name(), entry.occurredAt()))
            .toList();
    var actions =
        alert.status() == AlertStatus.RESOLVED ? List.of("RECORD_DISPOSITION") : List.<String>of();
    var recommendation =
        Optional.ofNullable(recommendations.getIfAvailable())
            .flatMap(service -> service.findFor(id))
            .map(OperationalCaseController::toRecommendation)
            .orElse(null);
    return new CaseDetail(
        caseId,
        new Lifecycle(
            alert.sourceId(),
            alert.status().name(),
            alert.runbook(),
            alert.triggeredAt(),
            alert.resolvedAt(),
            alert.updatedAt()),
        evidenceDto,
        List.copyOf(links),
        actions,
        timeline,
        recommendation);
  }

  private static void add(Set<String> links, String value) {
    if (value != null && !value.isBlank()) links.add(value);
  }

  private static RecommendationDto toRecommendation(RecommendationRecord record) {
    var result = record.result();
    return new RecommendationDto(
        record.status().name(),
        result == null ? null : result.recommendedDisposition(),
        result == null ? null : result.summary(),
        result == null ? null : result.proposedImprovement(),
        result == null ? List.of() : result.repositorySearchTerms(),
        result == null ? List.of() : result.citations(),
        result == null ? null : result.uncertainty(),
        record.generatedAt(),
        result == null ? null : result.model(),
        record.promptVersion(),
        record.status().name().equals("STALE"));
  }

  public record CaseDetail(
      UUID caseId,
      Lifecycle lifecycle,
      EvidenceDto evidence,
      List<String> sourceLinks,
      List<String> availableActions,
      List<TimelineEntry> timeline,
      RecommendationDto recommendation) {}

  public record Lifecycle(
      String sourceId,
      String status,
      String runbook,
      Instant triggeredAt,
      Instant resolvedAt,
      Instant updatedAt) {}

  public record EvidenceDto(
      String monitorId,
      Long durationSeconds,
      String severity,
      Map<String, Integer> recurrence,
      Freshness freshness) {}

  public record Freshness(Instant observedAt, boolean stale) {}

  public record TimelineEntry(String eventType, String actorType, Instant occurredAt) {}

  public record RecommendationDto(
      String status,
      String recommendedDisposition,
      String summary,
      String proposedImprovement,
      List<String> repositorySearchTerms,
      List<String> citations,
      String uncertainty,
      Instant generatedAt,
      String model,
      String promptVersion,
      boolean stale) {}
}
