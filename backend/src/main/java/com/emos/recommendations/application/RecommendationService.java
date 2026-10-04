package com.emos.recommendations.application;

import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.domain.OperationalCaseId;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "emos.recommendations.enabled", havingValue = "true")
public class RecommendationService {
    public static final String PROMPT_VERSION = "operational-case-recommendation-v1";
    private final EvidenceQueryService evidence;
    private final RecommendationRepository recommendations;
    private final RecommendationProvider provider;
    private final Clock clock;

    public RecommendationService(EvidenceQueryService evidence, RecommendationRepository recommendations,
                                 RecommendationProvider provider, Clock clock) {
        this.evidence = evidence; this.recommendations = recommendations; this.provider = provider; this.clock = clock;
    }

    public RecommendationRecord requestFor(OperationalCaseId caseId) {
        var snapshot = evidence.findLatest(caseId).orElseThrow(() -> new IllegalArgumentException("Evidence is required"));
        var version = version(snapshot);
        var key = caseId.value() + ":" + PROMPT_VERSION + ":" + version;
        return recommendations.findByKey(key).orElseGet(() -> recommendations.insert(new RecommendationRecord(
                UUID.randomUUID(), key, caseId, version, PROMPT_VERSION, RecommendationStatus.PENDING,
                null, clock.instant(), null, null)));
    }

    public void generate(UUID recommendationId) {
        var record = recommendations.findById(recommendationId).orElseThrow();
        if (record.status() != RecommendationStatus.PENDING) return;
        try {
            var snapshot = evidence.findLatest(record.caseId()).orElseThrow();
            var requestEvidence = List.of(new RecommendationRequest.Evidence(
                    String.join(",", snapshot.providerOccurrenceIds()), render(snapshot)));
            recommendations.replace(record.withReady(provider.generate(
                    new RecommendationRequest(record.caseId().value().toString(), requestEvidence)), clock.instant()));
        } catch (RuntimeException failure) {
            recommendations.replace(record.withFailed("Recommendation generation failed", clock.instant()));
        }
    }

    public Optional<RecommendationRecord> findFor(OperationalCaseId caseId) {
        return recommendations.findLatest(caseId).map(record -> {
            if (record.status() != RecommendationStatus.READY) return record;
            var current = evidence.findLatest(caseId).map(RecommendationService::version).orElse("missing");
            return current.equals(record.evidenceVersion()) ? record : record.asStale();
        });
    }

    private static String version(EvidenceQueryService.EvidenceSummary value) {
        var material = value.observedAt() + ":" + String.join(",", value.providerOccurrenceIds());
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String render(EvidenceQueryService.EvidenceSummary value) {
        return "monitor=" + value.monitorId() + " severity=" + value.severity() + " durationSeconds="
                + value.durationSeconds() + " recurrence24h=" + value.recurrence().last24Hours()
                + " recurrence7d=" + value.recurrence().last7Days() + " recurrence30d="
                + value.recurrence().last30Days() + " sourceLinks=" + value.sourceLinks();
    }
}
