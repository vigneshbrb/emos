package com.emos.web;

import com.emos.improvementknowledge.application.*;
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
public class CreateImprovementController {
  private final CreateImprovementService service;

  public CreateImprovementController(CreateImprovementService service) {
    this.service = service;
  }

  @PostMapping("/{caseId}/dispositions/create-improvement")
  CreateImprovementResult create(@PathVariable UUID caseId, @RequestBody Request request) {
    return service.execute(
        new CreateImprovementCommand(
            new OperationalCaseId(caseId),
            request.draftId(),
            request.attemptId(),
            request.reviewDate()));
  }

  record Request(UUID draftId, UUID attemptId, LocalDate reviewDate) {}
}
