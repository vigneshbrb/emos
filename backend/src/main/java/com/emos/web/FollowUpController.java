package com.emos.web;

import com.emos.improvementknowledge.application.*;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.*;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class FollowUpController {
  private final FollowUpRepository followUps;
  private final Clock clock;

  public FollowUpController(FollowUpRepository f, Clock c) {
    followUps = f;
    clock = c;
  }

  @GetMapping("/api/follow-ups/{id}")
  View get(@PathVariable UUID id) {
    return view(followUps.find(new FollowUpId(id)).orElseThrow());
  }

  @GetMapping("/api/cases/{caseId}/follow-up")
  View forCase(@PathVariable UUID caseId) {
    return view(followUps.findForCase(new OperationalCaseId(caseId)).orElseThrow());
  }

  private View view(FollowUpRecord r) {
    var stale =
        r.observedAt() == null
            || r.observedAt().isBefore(clock.instant().minus(Duration.ofMinutes(15)));
    return new View(
        r.id().value(),
        r.issue().key(),
        r.issue().url(),
        r.jiraStatus(),
        r.observedAt(),
        stale,
        r.reviewDate(),
        r.state(),
        r.state().equals("OPEN") && r.reviewDate().isBefore(LocalDate.now(clock)),
        r.finalRationaleRequired());
  }

  record View(
      UUID id,
      String jiraKey,
      String jiraUrl,
      String jiraStatus,
      Instant observedAt,
      boolean stale,
      LocalDate reviewDate,
      String state,
      boolean overdue,
      boolean finalRationaleRequired) {}
}
