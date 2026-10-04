package com.emos.attentionfollowthrough.infrastructure;

import com.emos.attentionfollowthrough.application.DeadlineEvaluationService;
import com.emos.platform.jobs.Job;
import com.emos.platform.jobs.JobHandler;
import com.emos.platform.jobs.JobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class DeadlineSweepJobHandler implements JobHandler {
    private final DeadlineEvaluationService deadlines;
    private final JobRepository jobs;
    private final ObjectMapper mapper;
    private final Clock clock;

    DeadlineSweepJobHandler(DeadlineEvaluationService deadlines, JobRepository jobs, ObjectMapper mapper, Clock clock) {
        this.deadlines = deadlines;
        this.jobs = jobs;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override public String type() { return "attention.deadline-sweep"; }

    @Override public void handle(Job job) {
        deadlines.evaluateDue(clock.instant());
        enqueue(clock.instant().plus(Duration.ofMinutes(5)));
    }

    @EventListener(ApplicationReadyEvent.class) void seed() { enqueue(clock.instant()); }

    private void enqueue(Instant at) {
        jobs.enqueue(type(), "attention-deadline-sweep:" + at, mapper.createObjectNode(), at);
    }
}
