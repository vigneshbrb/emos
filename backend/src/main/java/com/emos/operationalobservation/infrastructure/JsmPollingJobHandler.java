package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.JsmPollService;
import com.emos.platform.jobs.Job;
import com.emos.platform.jobs.JobHandler;
import com.emos.platform.jobs.JobRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(
    name = "emos.jsm.scheduling-enabled",
    havingValue = "true",
    matchIfMissing = true)
class JsmPollingJobHandler implements JobHandler {
  private final JsmPollService polling;
  private final JobRepository jobs;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Duration interval;

  JsmPollingJobHandler(
      JsmPollService polling,
      JobRepository jobs,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${emos.jsm.poll-interval:PT2M}") Duration interval) {
    this.polling = polling;
    this.jobs = jobs;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.interval = interval;
  }

  @Override
  public String type() {
    return "jsm.poll";
  }

  @Override
  public void handle(Job job) {
    polling.poll();
    enqueue(clock.instant().plus(interval));
  }

  @EventListener(ApplicationReadyEvent.class)
  void seedFirstPoll() {
    enqueue(clock.instant());
  }

  private void enqueue(Instant at) {
    jobs.enqueue(type(), "jsm-poll:" + at, objectMapper.createObjectNode(), at);
  }
}
