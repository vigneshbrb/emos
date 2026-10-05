package com.emos.recommendations.infrastructure;

import com.emos.operationalobservation.application.AlertResolved;
import com.emos.operationalobservation.domain.OperationalCaseId;
import com.emos.platform.jobs.*;
import com.emos.recommendations.application.RecommendationService;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "emos.recommendations.enabled", havingValue = "true")
class RecommendationJobHandler implements JobHandler {
  private final RecommendationService service;
  private final JobRepository jobs;
  private final ObjectMapper mapper;
  private final Clock clock;

  RecommendationJobHandler(
      RecommendationService service, JobRepository jobs, ObjectMapper mapper, Clock clock) {
    this.service = service;
    this.jobs = jobs;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  public String type() {
    return "recommendation.generate";
  }

  @Override
  public void handle(Job job) {
    var caseId = new OperationalCaseId(UUID.fromString(job.payload().path("caseId").stringValue()));
    var record = service.requestFor(caseId);
    service.generate(record.id());
  }

  @EventListener
  void on(AlertResolved event) {
    var payload = mapper.createObjectNode().put("caseId", event.caseId().value().toString());
    jobs.enqueue(
        type(),
        "recommendation:" + event.caseId().value() + ":" + event.resolvedAt(),
        payload,
        clock.instant().plus(Duration.ofMinutes(1)));
  }
}
