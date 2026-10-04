package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.Alert;
import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceEnrichmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");

    @Test
    void derives_exact_monitor_windows_duration_and_preserves_jsm_facts() {
        var alert = Alert.from(new JsmAlertSnapshot("alert-42", NOW, AlertStatus.ACTIVE,
                "https://jsm.example/alerts/42", "https://app.datadoghq.com/monitors/17?from_ts=1",
                "Inspect latency", "P2", NOW.minusSeconds(1_800), null));
        var alerts = new StubAlerts(alert);
        var evidence = new MonitorEvidence("17", "https://app.datadoghq.com/monitors/17", List.of(
                transition("30d", "17", NOW.minusSeconds(30L * 86_400), MonitorTransition.State.ALERT),
                transition("7d", "17", NOW.minusSeconds(7L * 86_400), MonitorTransition.State.ALERT),
                transition("24h", "17", NOW.minusSeconds(86_400), MonitorTransition.State.ALERT),
                transition("recent", "17", NOW.minusSeconds(900), MonitorTransition.State.ALERT),
                transition("recovery", "17", NOW.minusSeconds(300), MonitorTransition.State.OK),
                transition("wrong-monitor", "99", NOW.minusSeconds(60), MonitorTransition.State.ALERT)));
        var snapshots = new InMemoryEvidenceRepository();
        var service = new EvidenceEnrichmentService(alerts, (monitorId, asOf) -> evidence,
                snapshots, Clock.fixed(NOW, ZoneOffset.UTC));

        var snapshot = service.enrich(alert.caseId());

        assertThat(snapshot.monitorId()).isEqualTo("17");
        assertThat(snapshot.sourceLinks()).containsExactly(
                "https://jsm.example/alerts/42", "https://app.datadoghq.com/monitors/17?from_ts=1");
        assertThat(snapshot.jsmSeverity()).isEqualTo("P2");
        assertThat(snapshot.latestDuration()).isEqualTo(java.time.Duration.ofMinutes(10));
        assertThat(snapshot.recurrence()).isEqualTo(new RecurrenceCounts(2, 3, 4));
        assertThat(snapshot.providerOccurrenceIds()).containsExactlyInAnyOrder("30d", "7d", "24h", "recent", "recovery");
        assertThat(snapshots.saved).containsExactly(snapshot);
    }

    @Test
    void repeated_enrichment_of_same_provider_occurrences_returns_existing_immutable_snapshot() {
        var alert = Alert.from(new JsmAlertSnapshot("alert-42", NOW, AlertStatus.ACTIVE,
                "https://jsm.example/alerts/42", "https://app.datadoghq.com/monitors/17",
                "Inspect latency", "P2", NOW.minusSeconds(600), null));
        var snapshots = new InMemoryEvidenceRepository();
        var evidence = new MonitorEvidence("17", alert.monitorUrl(), List.of(
                transition("a", "17", NOW.minusSeconds(600), MonitorTransition.State.ALERT)));
        var service = new EvidenceEnrichmentService(new StubAlerts(alert), (id, at) -> evidence,
                snapshots, Clock.fixed(NOW, ZoneOffset.UTC));

        var first = service.enrich(alert.caseId());
        var second = service.enrich(alert.caseId());

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(snapshots.saved).hasSize(1);
    }

    private static MonitorTransition transition(String id, String monitorId, Instant at,
                                                MonitorTransition.State state) {
        return new MonitorTransition(id, monitorId, at, state);
    }

    private static final class StubAlerts implements AlertRepository {
        private final Alert alert;
        private StubAlerts(Alert alert) { this.alert = alert; }
        @Override public Optional<Alert> findBySourceId(String sourceId) { return Optional.of(alert); }
        @Override public Optional<Alert> findByCaseId(OperationalCaseId caseId) {
            return alert.caseId().equals(caseId) ? Optional.of(alert) : Optional.empty();
        }
        @Override public void insert(Alert alert) { }
        @Override public void update(Alert alert) { }
    }

    private static final class InMemoryEvidenceRepository implements EvidenceRepository {
        private final List<EvidenceSnapshot> saved = new ArrayList<>();
        @Override public EvidenceSnapshot saveIfAbsent(EvidenceSnapshot snapshot) {
            return saved.stream().filter(existing -> existing.snapshotKey().equals(snapshot.snapshotKey()))
                    .findFirst().orElseGet(() -> { saved.add(snapshot); return snapshot; });
        }
    }
}
