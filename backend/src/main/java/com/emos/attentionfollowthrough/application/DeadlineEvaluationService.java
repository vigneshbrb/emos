package com.emos.attentionfollowthrough.application;

import com.emos.attentionfollowthrough.domain.AttentionItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;

@Service
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class DeadlineEvaluationService {
    private final ObligationRepository obligations;
    private final AttentionItemRepository attentionItems;

    public DeadlineEvaluationService(ObligationRepository obligations, AttentionItemRepository attentionItems) {
        this.obligations = obligations;
        this.attentionItems = attentionItems;
    }

    @Transactional
    public EvaluationResult evaluateDue(Instant now) {
        int breached = 0;
        for (var obligation : obligations.findPendingDue(now)) {
            if (obligations.markBreached(obligation.id(), now)) {
                attentionItems.insertIfAbsent(AttentionItem.missingDisposition(obligation, now));
                breached++;
            }
        }
        return new EvaluationResult(breached);
    }

    public record EvaluationResult(int breached) { }
}
