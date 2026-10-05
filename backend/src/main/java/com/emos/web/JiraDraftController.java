package com.emos.web;

import com.emos.improvementknowledge.application.JiraDraftService;
import com.emos.improvementknowledge.domain.JiraDraft;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JiraDraftController {
  private final JiraDraftService service;

  public JiraDraftController(JiraDraftService service) {
    this.service = service;
  }

  @GetMapping("/{caseId}/jira-draft")
  JiraDraft get(@PathVariable UUID caseId) {
    return service.prepare(new OperationalCaseId(caseId));
  }

  @PutMapping("/{caseId}/jira-draft")
  JiraDraft put(@PathVariable UUID caseId, @RequestBody DraftUpdate request) {
    var current = service.prepare(new OperationalCaseId(caseId));
    return service.update(
        new JiraDraft(
            current.id(),
            current.caseId(),
            request.title(),
            request.problemStatement(),
            current.evidenceSummary(),
            request.proposedDirection(),
            request.acceptanceIntent(),
            current.repositoryId(),
            request.reviewDate(),
            request.approved(),
            current.updatedAt()));
  }

  record DraftUpdate(
      String title,
      String problemStatement,
      String proposedDirection,
      String acceptanceIntent,
      LocalDate reviewDate,
      boolean approved) {}
}
