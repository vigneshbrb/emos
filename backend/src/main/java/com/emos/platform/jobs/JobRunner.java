package com.emos.platform.jobs;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class JobRunner {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(5);
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(30);
    private static final Duration MAX_BACKOFF = Duration.ofHours(1);

    private final JobRepository jobs;
    private final Map<String, JobHandler> handlers;
    private final Clock clock;

    public JobRunner(JobRepository jobs, List<JobHandler> handlers, Clock clock) {
        this.jobs = jobs;
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(JobHandler::type, Function.identity()));
        this.clock = clock;
    }

    public int runAvailable(int limit) {
        var claimed = jobs.claimAvailable(limit, clock.instant(), LEASE_DURATION);
        claimed.forEach(this::run);
        return claimed.size();
    }

    private void run(Job job) {
        try {
            var handler = handlers.get(job.type());
            if (handler == null) {
                throw new IllegalStateException("No handler registered");
            }
            handler.handle(job);
            jobs.markSucceeded(job.id(), clock.instant());
        } catch (RuntimeException failure) {
            jobs.markFailed(job.id(), clock.instant().plus(backoff(job.attempts())), "Job handler failed");
        }
    }

    private Duration backoff(int attempts) {
        var exponent = Math.min(Math.max(attempts - 1, 0), 7);
        var seconds = INITIAL_BACKOFF.toSeconds() * (1L << exponent);
        return Duration.ofSeconds(Math.min(seconds, MAX_BACKOFF.toSeconds()));
    }
}
