package com.emos.attentionfollowthrough.application;

import com.emos.attentionfollowthrough.domain.Obligation;
import com.emos.attentionfollowthrough.domain.ObligationId;
import com.emos.attentionfollowthrough.domain.WorkingCalendar;
import com.emos.operationalobservation.application.AlertReopened;
import com.emos.operationalobservation.application.AlertResolved;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class DispositionObligationService {
    private final ObligationRepository obligations;
    private final WorkingCalendar calendar;

    public DispositionObligationService(ObligationRepository obligations, WorkingCalendar calendar) {
        this.obligations = obligations;
        this.calendar = calendar;
    }

    @Transactional
    public ObligationId onAlertResolved(OperationalCaseId caseId, Instant resolvedAt) {
        return obligations.findForResolution(caseId, resolvedAt).map(Obligation::id)
                .orElseGet(() -> obligations.insert(Obligation.pending(caseId, resolvedAt,
                        calendar.addWorkingHours(resolvedAt, Duration.ofHours(24)))).id());
    }

    @Transactional
    public void onAlertReopened(OperationalCaseId caseId, Instant reopenedAt) {
        obligations.cancelPending(caseId, reopenedAt);
    }

    @EventListener public void on(AlertResolved event) { onAlertResolved(event.caseId(), event.resolvedAt()); }
    @EventListener public void on(AlertReopened event) { onAlertReopened(event.caseId(), event.reopenedAt()); }
}
