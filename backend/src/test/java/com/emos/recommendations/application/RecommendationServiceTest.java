package com.emos.recommendations.application;

import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.application.RecurrenceCounts;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    @Test
    void pending_becomes_ready_and_repeated_request_deduplicates_prompt_and_evidence_version() {
        var caseId = OperationalCaseId.newId();
        var evidence = new MutableEvidence(caseId, snapshot(NOW, "occ-1"));
        var repository = new InMemoryRecommendations();
        RecommendationProvider provider = request -> new RecommendationResult("CREATE_IMPROVEMENT", "Recurring latency",
                "Profile query", List.of("latency"), List.of("occ-1"), "medium", "configured-model");
        var service = new RecommendationService(evidence, repository, provider, Clock.fixed(NOW, ZoneOffset.UTC));

        var pending = service.requestFor(caseId);
        var duplicate = service.requestFor(caseId);
        service.generate(pending.id());

        assertThat(duplicate.id()).isEqualTo(pending.id());
        assertThat(repository.records).hasSize(1);
        assertThat(service.findFor(caseId).orElseThrow().status()).isEqualTo(RecommendationStatus.READY);
    }

    @Test
    void provider_or_schema_failure_is_failed_and_does_not_escape_the_job_boundary() {
        var caseId = OperationalCaseId.newId();
        var service = new RecommendationService(new MutableEvidence(caseId, snapshot(NOW, "occ-1")),
                new InMemoryRecommendations(), request -> { throw new IllegalArgumentException("invalid schema"); },
                Clock.fixed(NOW, ZoneOffset.UTC));
        var pending = service.requestFor(caseId);

        service.generate(pending.id());

        assertThat(service.findFor(caseId).orElseThrow().status()).isEqualTo(RecommendationStatus.FAILED);
    }

    @Test
    void evidence_change_marks_prior_ready_output_stale_without_changing_its_original_result() {
        var caseId = OperationalCaseId.newId();
        var evidence = new MutableEvidence(caseId, snapshot(NOW, "occ-1"));
        var service = new RecommendationService(evidence, new InMemoryRecommendations(), request ->
                new RecommendationResult("CREATE_IMPROVEMENT", "Summary", "Proposal", List.of(), List.of("occ-1"), "low", "model"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        var pending = service.requestFor(caseId); service.generate(pending.id());

        evidence.current = snapshot(NOW.plusSeconds(60), "occ-2");

        assertThat(service.findFor(caseId).orElseThrow().status()).isEqualTo(RecommendationStatus.STALE);
        assertThat(service.findFor(caseId).orElseThrow().result().summary()).isEqualTo("Summary");
    }

    private static EvidenceQueryService.EvidenceSummary snapshot(Instant at, String occurrence) {
        return new EvidenceQueryService.EvidenceSummary(at, "17", "P2", 600L,
                new RecurrenceCounts(3, 7, 12), List.of(occurrence), List.of("https://datadog/17"));
    }

    private static final class MutableEvidence implements EvidenceQueryService {
        private final OperationalCaseId id; private EvidenceSummary current;
        private MutableEvidence(OperationalCaseId id, EvidenceSummary current) { this.id=id; this.current=current; }
        @Override public Optional<EvidenceSummary> findLatest(OperationalCaseId caseId) {
            return id.equals(caseId) ? Optional.of(current) : Optional.empty();
        }
    }

    private static final class InMemoryRecommendations implements RecommendationRepository {
        private final List<RecommendationRecord> records = new ArrayList<>();
        @Override public Optional<RecommendationRecord> findByKey(String key) { return records.stream().filter(r -> r.deduplicationKey().equals(key)).findFirst(); }
        @Override public Optional<RecommendationRecord> findById(UUID id) { return records.stream().filter(r -> r.id().equals(id)).findFirst(); }
        @Override public Optional<RecommendationRecord> findLatest(OperationalCaseId id) { return records.stream().filter(r -> r.caseId().equals(id)).reduce((a,b)->b); }
        @Override public RecommendationRecord insert(RecommendationRecord record) { records.add(record); return record; }
        @Override public void replace(RecommendationRecord record) { records.replaceAll(old -> old.id().equals(record.id()) ? record : old); }
    }
}
