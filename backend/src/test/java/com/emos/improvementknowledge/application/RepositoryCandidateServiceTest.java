package com.emos.improvementknowledge.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class RepositoryCandidateServiceTest {
  @Test
  void confirmed_mapping_is_authoritative_and_metadata_precedes_ai_only_hints() {
    var caseId = new OperationalCaseId(UUID.randomUUID());
    var catalog =
        List.of(
            new RepositoryDocument(
                "payments",
                "Payment latency service",
                Set.of("sql", "latency"),
                List.of("README: A11 SQL"),
                "r1",
                Instant.now(),
                true),
            new RepositoryDocument(
                "web", "Frontend", Set.of(), List.of(), "r2", Instant.now(), true));
    var service =
        new RepositoryCandidateService(
            id -> catalog,
            id -> Optional.of(new RepositoryMapping("monitor-1", "web", "confirmed", true)),
            id ->
                new RepositoryCandidateService.CaseSignals(
                    "monitor-1", List.of("sql", "latency"), List.of("web")));

    var candidates = service.candidatesFor(caseId);

    assertThat(candidates)
        .extracting(RepositoryCandidate::repositoryId)
        .containsExactly("web", "payments");
    assertThat(candidates.getFirst().authoritative()).isTrue();
    assertThat(candidates).allSatisfy(candidate -> assertThat(candidate.reasons()).isNotEmpty());
  }

  @Test
  void inaccessible_confirmed_mapping_requires_confirmation_instead_of_fallback() {
    var caseId = new OperationalCaseId(UUID.randomUUID());
    var service =
        new RepositoryCandidateService(
            id -> List.of(),
            id -> Optional.of(new RepositoryMapping("monitor-1", "gone", "confirmed", false)),
            id -> new RepositoryCandidateService.CaseSignals("monitor-1", List.of(), List.of()));
    assertThat(service.candidatesFor(caseId).getFirst())
        .satisfies(
            candidate -> {
              assertThat(candidate.repositoryId()).isEqualTo("gone");
              assertThat(candidate.accessible()).isFalse();
              assertThat(candidate.requiresConfirmation()).isTrue();
            });
  }
}
