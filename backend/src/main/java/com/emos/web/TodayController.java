package com.emos.web;

import com.emos.attentionfollowthrough.application.AttentionQueryService;
import com.emos.operationalobservation.application.AlertQueryService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/today")
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class TodayController {
  private final AttentionQueryService attention;
  private final AlertQueryService alerts;

  public TodayController(AttentionQueryService attention, AlertQueryService alerts) {
    this.attention = attention;
    this.alerts = alerts;
  }

  @GetMapping
  TodayResponse today() {
    var attentionNow =
        attention.findOpen().stream()
            .map(
                item -> {
                  var alert = alerts.findByCaseId(item.caseId()).orElseThrow();
                  return new AttentionNow(
                      item.caseId().value(),
                      item.reason(),
                      item.deadline(),
                      false,
                      alert.severity(),
                      alert.sourceUrl());
                })
            .sorted(
                Comparator.comparing(AttentionNow::deadline)
                    .thenComparing(AttentionNow::confirmedIncident, Comparator.reverseOrder())
                    .thenComparingInt(item -> severityRank(item.severity()))
                    .thenComparing(item -> item.caseId().toString()))
            .toList();
    var pending =
        attention.findPending().stream()
            .map(item -> new Pending(item.caseId().value(), item.deadline()))
            .toList();
    var active =
        alerts.findActive().stream()
            .map(
                item ->
                    new ActiveSignal(
                        item.caseId().value(),
                        item.sourceId(),
                        item.severity(),
                        item.sourceUrl(),
                        item.updatedAt()))
            .toList();
    return new TodayResponse(attentionNow, pending, active);
  }

  private static int severityRank(String severity) {
    if (severity == null) return Integer.MAX_VALUE;
    return switch (severity.toUpperCase()) {
      case "P1" -> 1;
      case "P2" -> 2;
      case "P3" -> 3;
      default -> 99;
    };
  }

  public record TodayResponse(
      List<AttentionNow> attentionNow, List<Pending> pending, List<ActiveSignal> activeSignals) {}

  public record AttentionNow(
      UUID caseId,
      String reason,
      Instant deadline,
      boolean confirmedIncident,
      String severity,
      String sourceUrl) {}

  public record Pending(UUID caseId, Instant deadline) {}

  public record ActiveSignal(
      UUID caseId, String sourceId, String severity, String sourceUrl, Instant updatedAt) {}
}
