package com.emos.attentionfollowthrough.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.emos.attentionfollowthrough.domain.ObligationState;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos-obligations",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class DispositionObligationServiceIntegrationTest {
  @Autowired DispositionObligationService service;
  @Autowired AttentionQueryService queries;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute("truncate table attention_item, disposition_obligation");
  }

  @Test
  void resolution_creates_one_pending_obligation_from_source_time_and_replay_is_idempotent() {
    var caseId = OperationalCaseId.newId();
    var resolvedAt = Instant.parse("2026-10-09T04:30:00Z"); // Friday 10:00 India

    var first = service.onAlertResolved(caseId, resolvedAt);
    var replay = service.onAlertResolved(caseId, resolvedAt);

    assertThat(replay).isEqualTo(first);
    assertThat(queries.findPending())
        .singleElement()
        .satisfies(
            pending -> {
              assertThat(pending.caseId()).isEqualTo(caseId);
              assertThat(pending.state()).isEqualTo(ObligationState.PENDING);
              assertThat(pending.deadline()).isEqualTo(Instant.parse("2026-10-12T04:30:00Z"));
            });
  }

  @Test
  void reopening_cancels_the_pending_obligation() {
    var caseId = OperationalCaseId.newId();
    service.onAlertResolved(caseId, Instant.parse("2026-10-05T04:30:00Z"));

    service.onAlertReopened(caseId, Instant.parse("2026-10-05T05:00:00Z"));

    assertThat(queries.findPending()).isEmpty();
    assertThat(jdbc.queryForObject("select state from disposition_obligation", String.class))
        .isEqualTo("CANCELLED");
  }
}
