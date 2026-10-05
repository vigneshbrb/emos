package com.emos.web;

import com.emos.improvementknowledge.application.*;
import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cases")
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RepositoryMappingController {
  private final RepositoryCandidateService candidates;
  private final RepositoryMappingService mappings;
  private final EvidenceQueryService evidence;

  public RepositoryMappingController(
      RepositoryCandidateService candidates,
      RepositoryMappingService mappings,
      EvidenceQueryService evidence) {
    this.candidates = candidates;
    this.mappings = mappings;
    this.evidence = evidence;
  }

  @GetMapping("/{caseId}/repository-candidates")
  List<RepositoryCandidate> candidates(@PathVariable UUID caseId) {
    return candidates.candidatesFor(new OperationalCaseId(caseId));
  }

  @PostMapping("/{caseId}/repository-confirmation")
  Map<String, UUID> confirm(@PathVariable UUID caseId, @RequestBody Confirmation request) {
    var id = new OperationalCaseId(caseId);
    var observed =
        evidence
            .findLatest(id)
            .orElseThrow(() -> new IllegalArgumentException("Evidence is required"));
    if (!observed.monitorId().equals(request.monitorId()))
      throw new IllegalArgumentException("Monitor does not match case evidence");
    return Map.of(
        "mappingId",
        mappings
            .confirmMonitorMapping(
                id,
                request.monitorId(),
                new RepositoryRef(request.repositoryId()),
                request.rationale())
            .value());
  }

  public record Confirmation(String monitorId, String repositoryId, String rationale) {}
}
