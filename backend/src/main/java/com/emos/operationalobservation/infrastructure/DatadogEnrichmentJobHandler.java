package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.AlertResolved;
import com.emos.operationalobservation.application.EvidenceEnrichmentService;
import com.emos.operationalobservation.domain.OperationalCaseId;
import com.emos.platform.jobs.Job;
import com.emos.platform.jobs.JobHandler;
import com.emos.platform.jobs.JobRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "emos.datadog.enabled", havingValue = "true")
class DatadogEnrichmentJobHandler implements JobHandler {
  private final EvidenceEnrichmentService enrichment;
  private final JobRepository jobs;
  private final ObjectMapper mapper;
  private final Clock clock;

  DatadogEnrichmentJobHandler(
      EvidenceEnrichmentService enrichment, JobRepository jobs, ObjectMapper mapper, Clock clock) {
    this.enrichment = enrichment;
    this.jobs = jobs;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  public String type() {
    return "datadog.enrich";
  }

  @Override
  public void handle(Job job) {
    enrichment.enrich(
        new OperationalCaseId(UUID.fromString(job.payload().path("caseId").stringValue())));
  }

  @EventListener
  void on(AlertResolved event) {
    var payload = mapper.createObjectNode().put("caseId", event.caseId().value().toString());
    jobs.enqueue(
        type(),
        "datadog-enrich:" + event.caseId().value() + ":" + event.resolvedAt(),
        payload,
        clock.instant());
  }
}
