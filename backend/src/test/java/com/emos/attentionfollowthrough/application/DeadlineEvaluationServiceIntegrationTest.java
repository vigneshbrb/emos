package com.emos.attentionfollowthrough.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:17:///emos-deadlines",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
class DeadlineEvaluationServiceIntegrationTest {
    @Autowired DispositionObligationService obligations;
    @Autowired DeadlineEvaluationService deadlines;
    @Autowired AttentionQueryService queries;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() { jdbc.execute("truncate table attention_item, disposition_obligation"); }

    @Test
    void first_sweep_after_laptop_downtime_breaches_due_obligation_and_creates_one_attention_item() {
        var caseId = OperationalCaseId.newId();
        obligations.onAlertResolved(caseId, Instant.parse("2026-10-05T04:30:00Z"));

        var result = deadlines.evaluateDue(Instant.parse("2026-10-08T00:00:00Z"));
        var replay = deadlines.evaluateDue(Instant.parse("2026-10-08T01:00:00Z"));

        assertThat(result.breached()).isEqualTo(1);
        assertThat(replay.breached()).isZero();
        assertThat(queries.findPending()).isEmpty();
        assertThat(queries.findOpen()).singleElement().satisfies(item -> {
            assertThat(item.caseId()).isEqualTo(caseId);
            assertThat(item.reason()).isEqualTo("Improvement decision missing");
        });
        assertThat(jdbc.queryForObject("select count(*) from attention_item", Integer.class)).isEqualTo(1);
    }
}
