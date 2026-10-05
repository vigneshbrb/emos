package com.emos.platform.jobs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos-jobs",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class JobRunnerIntegrationTest {

  private static final Instant START = Instant.parse("2026-10-04T06:30:00Z");

  @Autowired JobRepository jobs;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;

  private MutableClock clock;

  @BeforeEach
  void cleanDatabase() {
    jdbc.update("delete from platform_job");
    clock = new MutableClock(START);
  }

  @Test
  void enqueue_deduplicates_by_type_and_key() {
    var first =
        jobs.enqueue("poll", "jsm-42", objectMapper.createObjectNode().put("page", 1), START);
    var duplicate =
        jobs.enqueue("poll", "jsm-42", objectMapper.createObjectNode().put("page", 2), START);

    assertThat(duplicate.id()).isEqualTo(first.id());
    assertThat(jobs.findAll()).hasSize(1);
  }

  @Test
  void claimed_job_is_reclaimed_after_lease_expiry() {
    jobs.enqueue("poll", "jsm-42", objectMapper.createObjectNode(), START);
    var firstClaim = jobs.claimAvailable(1, START, Duration.ofMinutes(5));

    assertThat(firstClaim).singleElement().extracting(Job::state).isEqualTo(Job.State.RUNNING);
    assertThat(jobs.claimAvailable(1, START.plus(Duration.ofMinutes(4)), Duration.ofMinutes(5)))
        .isEmpty();

    var reclaimed =
        jobs.claimAvailable(1, START.plus(Duration.ofMinutes(6)), Duration.ofMinutes(5));
    assertThat(reclaimed)
        .singleElement()
        .satisfies(
            job -> {
              assertThat(job.state()).isEqualTo(Job.State.RUNNING);
              assertThat(job.attempts()).isEqualTo(2);
            });
  }

  @Test
  void successful_job_is_not_run_twice() {
    var calls = new AtomicInteger();
    jobs.enqueue("poll", "jsm-42", objectMapper.createObjectNode(), START);
    var runner = new JobRunner(jobs, List.of(handler("poll", calls, false)), clock);

    assertThat(runner.runAvailable(10)).isEqualTo(1);
    assertThat(runner.runAvailable(10)).isZero();
    assertThat(calls).hasValue(1);
    assertThat(jobs.findAll())
        .singleElement()
        .extracting(Job::state)
        .isEqualTo(Job.State.SUCCEEDED);
  }

  @Test
  void failed_job_retries_with_backoff() {
    var calls = new AtomicInteger();
    jobs.enqueue("poll", "jsm-42", objectMapper.createObjectNode(), START);
    var runner = new JobRunner(jobs, List.of(handler("poll", calls, true)), clock);

    assertThat(runner.runAvailable(10)).isEqualTo(1);
    var failed = jobs.findAll().getFirst();
    assertThat(failed.state()).isEqualTo(Job.State.READY);
    assertThat(failed.availableAt()).isEqualTo(START.plusSeconds(30));
    assertThat(runner.runAvailable(10)).isZero();

    clock.advance(Duration.ofSeconds(30));
    assertThat(runner.runAvailable(10)).isEqualTo(1);
    assertThat(jobs.findAll().getFirst().availableAt()).isEqualTo(START.plusSeconds(90));
  }

  private static JobHandler handler(String type, AtomicInteger calls, boolean fail) {
    return new JobHandler() {
      @Override
      public String type() {
        return type;
      }

      @Override
      public void handle(Job job) {
        calls.incrementAndGet();
        if (fail) throw new IllegalStateException("provider unavailable");
      }
    };
  }

  private static final class MutableClock extends Clock {
    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration duration) {
      instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
