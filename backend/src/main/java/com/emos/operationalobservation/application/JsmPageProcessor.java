package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.Alert;
import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.platform.audit.AuditEntry;
import com.emos.platform.audit.AuditTrail;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JsmPageProcessor {
    private final AlertRepository alerts;
    private final SourceEventRepository sourceEvents;
    private final JsmPollStateRepository pollState;
    private final AuditTrail auditTrail;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;

    JsmPageProcessor(AlertRepository alerts, SourceEventRepository sourceEvents, JsmPollStateRepository pollState,
                     AuditTrail auditTrail, ApplicationEventPublisher events, ObjectMapper objectMapper) {
        this.alerts = alerts;
        this.sourceEvents = sourceEvents;
        this.pollState = pollState;
        this.auditTrail = auditTrail;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    JsmPollService.PollResult process(JsmAlertPage page, JsmPollStateRepository.PollState state) {
        page.alerts().forEach(record -> {
            if (record.rawPayload() == null) throw new IllegalArgumentException("Raw JSM payload is required");
        });
        int processed = 0;
        int duplicates = 0;
        int ignored = 0;
        var maximumUpdate = state.updatedSince();
        for (var record : page.alerts()) {
            var snapshot = record.snapshot();
            if (snapshot.updatedAt().isAfter(maximumUpdate)) maximumUpdate = snapshot.updatedAt();
            var sourceEventId = sourceEvents.insert("JSM", snapshot.sourceId(), snapshot.updatedAt(), record.rawPayload());
            if (sourceEventId.isEmpty()) {
                duplicates++;
                continue;
            }
            var existing = alerts.findBySourceId(snapshot.sourceId());
            if (existing.isEmpty()) {
                var alert = Alert.from(snapshot);
                alerts.insert(alert);
                sourceEvents.markApplied(sourceEventId.get());
                appendAudit(alert, snapshot.status() == AlertStatus.RESOLVED ? "alert.resolved" : "alert.detected");
                if (snapshot.status() == AlertStatus.RESOLVED) {
                    events.publishEvent(new AlertResolved(alert.caseId(), alert.resolvedAt()));
                }
                processed++;
                continue;
            }
            var alert = existing.get();
            var change = alert.apply(snapshot);
            if (change.type() == Alert.AlertChange.Type.IGNORED) {
                sourceEvents.markIgnored(sourceEventId.get());
                ignored++;
                continue;
            }
            alerts.update(alert);
            sourceEvents.markApplied(sourceEventId.get());
            appendAudit(alert, eventType(change.type()));
            if (change.type() == Alert.AlertChange.Type.RESOLVED) {
                events.publishEvent(new AlertResolved(alert.caseId(), alert.resolvedAt()));
            } else if (change.type() == Alert.AlertChange.Type.REOPENED) {
                events.publishEvent(new AlertReopened(alert.caseId(), alert.lastSourceUpdatedAt()));
            }
            processed++;
        }
        var nextUpdatedSince = page.nextCursor() == null ? maximumUpdate : state.updatedSince();
        pollState.advance(page.nextCursor(), nextUpdatedSince);
        return new JsmPollService.PollResult(processed, duplicates, ignored, page.nextCursor());
    }

    private void appendAudit(Alert alert, String eventType) {
        var details = objectMapper.createObjectNode()
                .put("sourceId", alert.sourceId())
                .put("status", alert.status().name())
                .put("sourceUpdatedAt", alert.lastSourceUpdatedAt().toString());
        auditTrail.append(new AuditEntry(null, "ALERT", alert.caseId().value(), eventType,
                AuditEntry.ActorType.SOURCE, alert.lastSourceUpdatedAt(), details, List.of()));
    }

    private String eventType(Alert.AlertChange.Type type) {
        return switch (type) {
            case RESOLVED -> "alert.resolved";
            case REOPENED -> "alert.reopened";
            case UPDATED -> "alert.updated";
            case IGNORED -> throw new IllegalArgumentException("Ignored changes are not audited");
        };
    }
}
