package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "emos.datadog.enabled", havingValue = "true")
public class EvidenceEnrichmentService {
    private static final Pattern MONITOR_PATH = Pattern.compile("(?:^|/)monitors/(\\d+)(?:/|$)");
    private final AlertRepository alerts;
    private final DatadogClient datadog;
    private final EvidenceRepository snapshots;
    private final Clock clock;

    public EvidenceEnrichmentService(AlertRepository alerts, DatadogClient datadog,
                                     EvidenceRepository snapshots, Clock clock) {
        this.alerts = alerts;
        this.datadog = datadog;
        this.snapshots = snapshots;
        this.clock = clock;
    }

    public EvidenceSnapshot enrich(OperationalCaseId caseId) {
        var alert = alerts.findByCaseId(caseId).orElseThrow(() -> new IllegalArgumentException("Unknown case"));
        var monitorId = monitorId(alert.monitorUrl());
        var asOf = clock.instant();
        var raw = datadog.loadEvidence(monitorId, asOf);
        var transitions = raw.transitions().stream()
                .filter(t -> monitorId.equals(t.monitorId()))
                .filter(t -> !t.occurredAt().isAfter(asOf))
                .sorted(Comparator.comparing(MonitorTransition::occurredAt))
                .toList();
        var occurrences = transitions.stream().filter(EvidenceEnrichmentService::isTrigger).toList();
        var counts = new RecurrenceCounts(countSince(occurrences, asOf.minus(Duration.ofHours(24))),
                countSince(occurrences, asOf.minus(Duration.ofDays(7))),
                countSince(occurrences, asOf.minus(Duration.ofDays(30))));
        var ids = transitions.stream().map(MonitorTransition::providerOccurrenceId).distinct().sorted().toList();
        var keyMaterial = caseId.value() + ":" + monitorId + ":" + String.join(",", ids);
        var key = UUID.nameUUIDFromBytes(keyMaterial.getBytes(StandardCharsets.UTF_8)).toString();
        var snapshot = new EvidenceSnapshot(UUID.randomUUID(), key, caseId, asOf, monitorId,
                alert.severity(), latestDuration(transitions), counts, ids,
                nonNullLinks(alert.sourceUrl(), alert.monitorUrl()));
        return snapshots.saveIfAbsent(snapshot);
    }

    private static String monitorId(String url) {
        if (url == null) throw new IllegalArgumentException("Alert has no Datadog monitor URL");
        var matcher = MONITOR_PATH.matcher(URI.create(url).getPath());
        if (!matcher.find()) throw new IllegalArgumentException("Unsupported Datadog monitor URL");
        return matcher.group(1);
    }

    private static boolean isTrigger(MonitorTransition transition) {
        return transition.state() == MonitorTransition.State.ALERT
                || transition.state() == MonitorTransition.State.WARNING;
    }

    private static int countSince(List<MonitorTransition> transitions, Instant boundary) {
        return (int) transitions.stream().filter(t -> !t.occurredAt().isBefore(boundary)).count();
    }

    private static Duration latestDuration(List<MonitorTransition> transitions) {
        MonitorTransition trigger = null;
        Duration latest = null;
        for (var transition : transitions) {
            if (isTrigger(transition)) trigger = transition;
            else if (transition.state() == MonitorTransition.State.OK && trigger != null) {
                latest = Duration.between(trigger.occurredAt(), transition.occurredAt());
                trigger = null;
            }
        }
        return latest;
    }

    private static List<String> nonNullLinks(String first, String second) {
        return java.util.stream.Stream.of(first, second).filter(v -> v != null && !v.isBlank()).toList();
    }
}
