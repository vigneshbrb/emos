package com.emos.operationalobservation.application;

import com.emos.platform.diagnostics.IntegrationStatusRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@ConditionalOnProperty(name = "emos.jsm.enabled", havingValue = "true")
public class JsmPollService {
    private final JsmClient client;
    private final JsmPollStateRepository stateRepository;
    private final JsmPageProcessor processor;
    private final IntegrationStatusRepository statuses;
    private final Clock clock;

    JsmPollService(JsmClient client, JsmPollStateRepository stateRepository, JsmPageProcessor processor,
                   IntegrationStatusRepository statuses, Clock clock) {
        this.client = client;
        this.stateRepository = stateRepository;
        this.processor = processor;
        this.statuses = statuses;
        this.clock = clock;
    }

    public PollResult poll() {
        var state = stateRepository.current();
        try {
            var result = processor.process(client.fetchAlerts(state.updatedSince(), state.cursor()), state);
            statuses.recordSuccess("JSM", clock.instant());
            return result;
        } catch (RuntimeException failure) {
            statuses.recordFailure("JSM", clock.instant(), "JSM polling failed");
            throw failure;
        }
    }

    public record PollResult(int processed, int duplicates, int ignored, String nextCursor) {
    }
}
