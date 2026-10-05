package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.FollowUpRepository;
import com.emos.platform.jobs.*;
import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "emos.jira.enabled", havingValue = "true")
public class ImprovementReviewJobHandler implements JobHandler {
  private final FollowUpRepository followUps;
  private final JobRepository jobs;
  private final ObjectMapper json;
  private final Clock clock;

  public ImprovementReviewJobHandler(
      FollowUpRepository f, JobRepository j, ObjectMapper m, Clock c) {
    followUps = f;
    jobs = j;
    json = m;
    clock = c;
  }

  public String type() {
    return "improvement.review.scan";
  }

  public void handle(Job ignored) {
    for (var followUp : followUps.findOpen())
      jobs.enqueue(
          "jira.followup.refresh",
          "jira-followup:" + followUp.id().value() + ":" + LocalDate.now(clock),
          json.createObjectNode().put("followUpId", followUp.id().value().toString()),
          clock.instant());
    enqueueTomorrow();
  }

  @EventListener(ApplicationReadyEvent.class)
  void seed() {
    jobs.enqueue(
        type(),
        "improvement-review:" + LocalDate.now(clock),
        json.createObjectNode(),
        clock.instant());
  }

  private void enqueueTomorrow() {
    var tomorrow = LocalDate.now(clock).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    jobs.enqueue(
        type(),
        "improvement-review:" + LocalDate.now(clock).plusDays(1),
        json.createObjectNode(),
        tomorrow);
  }
}
