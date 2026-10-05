package com.emos.platform.jobs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class JobScheduler {
  private final JobRunner runner;

  JobScheduler(JobRunner runner) {
    this.runner = runner;
  }

  @Scheduled(fixedDelayString = "${emos.jobs.poll-delay:PT30S}")
  void runAvailableJobs() {
    runner.runAvailable(20);
  }
}
