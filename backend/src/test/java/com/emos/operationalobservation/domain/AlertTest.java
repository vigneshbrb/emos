package com.emos.operationalobservation.domain;

import com.emos.operationalobservation.application.JsmAlertSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTest {
    private static final Instant TRIGGERED = Instant.parse("2026-10-04T06:00:00Z");
    private static final Instant FIRST_UPDATE = Instant.parse("2026-10-04T06:05:00Z");
    private static final Instant RESOLVED = Instant.parse("2026-10-04T06:20:00Z");

    @Test
    void active_transitions_to_resolved() {
        var alert = Alert.from(active(FIRST_UPDATE));

        var change = alert.apply(resolved(RESOLVED));

        assertThat(change.type()).isEqualTo(Alert.AlertChange.Type.RESOLVED);
        assertThat(alert.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.resolvedAt()).isEqualTo(RESOLVED);
        assertThat(alert.lastSourceUpdatedAt()).isEqualTo(RESOLVED);
    }

    @Test
    void repeated_resolved_snapshot_is_idempotent() {
        var alert = Alert.from(active(FIRST_UPDATE));
        alert.apply(resolved(RESOLVED));

        var duplicate = alert.apply(resolved(RESOLVED));
        var laterResolved = alert.apply(resolved(RESOLVED.plusSeconds(60)));

        assertThat(duplicate.type()).isEqualTo(Alert.AlertChange.Type.IGNORED);
        assertThat(laterResolved.type()).isEqualTo(Alert.AlertChange.Type.UPDATED);
        assertThat(laterResolved.statusChanged()).isFalse();
        assertThat(alert.status()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void older_snapshot_cannot_regress_resolved_state() {
        var alert = Alert.from(active(FIRST_UPDATE));
        alert.apply(resolved(RESOLVED));

        var change = alert.apply(active(FIRST_UPDATE.plusSeconds(30)));

        assertThat(change.type()).isEqualTo(Alert.AlertChange.Type.IGNORED);
        assertThat(alert.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.lastSourceUpdatedAt()).isEqualTo(RESOLVED);
    }

    @Test
    void newer_active_snapshot_reopens_resolved_alert() {
        var alert = Alert.from(active(FIRST_UPDATE));
        alert.apply(resolved(RESOLVED));

        var reopenedAt = RESOLVED.plusSeconds(60);
        var change = alert.apply(active(reopenedAt));

        assertThat(change.type()).isEqualTo(Alert.AlertChange.Type.REOPENED);
        assertThat(change.statusChanged()).isTrue();
        assertThat(alert.status()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(alert.resolvedAt()).isNull();
        assertThat(alert.lastSourceUpdatedAt()).isEqualTo(reopenedAt);
    }

    @Test
    void snapshot_metadata_is_provider_neutral_and_updated_with_newer_facts() {
        var alert = Alert.from(active(FIRST_UPDATE));
        var changed = new JsmAlertSnapshot("JSM-42", FIRST_UPDATE.plusSeconds(60), AlertStatus.ACTIVE,
                "https://jsm.example/alerts/JSM-42", "https://datadog.example/monitor/17",
                "Check application latency", "P2", TRIGGERED, null);

        alert.apply(changed);

        assertThat(alert.sourceId()).isEqualTo("JSM-42");
        assertThat(alert.sourceUrl()).isEqualTo("https://jsm.example/alerts/JSM-42");
        assertThat(alert.monitorUrl()).isEqualTo("https://datadog.example/monitor/17");
        assertThat(alert.runbook()).isEqualTo("Check application latency");
        assertThat(alert.severity()).isEqualTo("P2");
        assertThat(alert.triggeredAt()).isEqualTo(TRIGGERED);
    }

    private static JsmAlertSnapshot active(Instant updatedAt) {
        return new JsmAlertSnapshot("JSM-42", updatedAt, AlertStatus.ACTIVE,
                "https://jsm.example/alerts/JSM-42", "https://datadog.example/monitor/17",
                "Check the latency dashboards", "P2", TRIGGERED, null);
    }

    private static JsmAlertSnapshot resolved(Instant updatedAt) {
        return new JsmAlertSnapshot("JSM-42", updatedAt, AlertStatus.RESOLVED,
                "https://jsm.example/alerts/JSM-42", "https://datadog.example/monitor/17",
                "Check the latency dashboards", "P2", TRIGGERED, updatedAt);
    }
}
