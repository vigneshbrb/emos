package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.AlertStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@RecordApplicationEvents
@Import(JsmPollServiceIntegrationTest.StubConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:17:///emos-jsm-poll",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "emos.jsm.enabled=true",
        "emos.jsm.scheduling-enabled=false"
})
class JsmPollServiceIntegrationTest {

    @Autowired JsmPollService pollService;
    @Autowired StubJsmClient jsm;
    @Autowired AlertQueryService alerts;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ApplicationEvents events;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("truncate table operational_source_event, operational_alert, platform_audit_entry");
        jdbc.update("update operational_jsm_poll_state set cursor = null, updated_since = ? where singleton = true",
                java.sql.Timestamp.from(Instant.EPOCH));
        jsm.clear();
    }

    @Test
    void replay_and_out_of_order_pages_do_not_duplicate_or_regress_outcomes() {
        var activeAt = Instant.parse("2026-10-04T06:05:00Z");
        var resolvedAt = Instant.parse("2026-10-04T06:20:00Z");
        jsm.add(page(record(snapshot(AlertStatus.ACTIVE, activeAt, null), "active"), "100"));
        jsm.add(page(record(snapshot(AlertStatus.RESOLVED, resolvedAt, resolvedAt), "resolved"), null));
        jsm.add(page(record(snapshot(AlertStatus.RESOLVED, resolvedAt, resolvedAt), "resolved"), null));
        jsm.add(page(record(snapshot(AlertStatus.ACTIVE, activeAt.plusSeconds(30), null), "late-active"), null));

        assertThat(pollService.poll().processed()).isEqualTo(1);
        assertThat(pollService.poll().processed()).isEqualTo(1);
        assertThat(pollService.poll().duplicates()).isEqualTo(1);
        assertThat(pollService.poll().ignored()).isEqualTo(1);

        assertThat(alerts.findActive()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from operational_alert", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from operational_source_event", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from operational_source_event where raw_payload ->> 'fixture' = 'resolved'", Integer.class)).isEqualTo(1);
        assertThat(events.stream(AlertResolved.class)).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from platform_audit_entry where event_type = 'alert.resolved'", Integer.class)).isEqualTo(1);
    }

    @Test
    void cursor_advances_only_after_every_record_in_the_page_is_processed() {
        jsm.add(new JsmAlertPage(List.of(
                record(snapshot(AlertStatus.ACTIVE, Instant.parse("2026-10-04T06:05:00Z"), null), "valid"),
                new JsmAlertRecord(snapshot(AlertStatus.ACTIVE, Instant.parse("2026-10-04T06:06:00Z"), null), null)
        ), "200"));

        assertThatThrownBy(pollService::poll).isInstanceOf(IllegalArgumentException.class);

        assertThat(jdbc.queryForObject("select cursor from operational_jsm_poll_state where singleton = true", String.class)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from operational_alert", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from operational_source_event", Integer.class)).isZero();
    }

    private JsmAlertPage page(JsmAlertRecord alert, String nextCursor) {
        return new JsmAlertPage(List.of(alert), nextCursor);
    }

    private JsmAlertRecord record(JsmAlertSnapshot snapshot, String fixture) {
        return new JsmAlertRecord(snapshot, objectMapper.createObjectNode()
                .put("id", snapshot.sourceId()).put("fixture", fixture));
    }

    private JsmAlertSnapshot snapshot(AlertStatus status, Instant updatedAt, Instant resolvedAt) {
        return new JsmAlertSnapshot("alert-42", updatedAt, status,
                "https://jsm.example/alerts/alert-42", "https://app.datadoghq.com/monitors/17",
                "Check latency dashboards", "P2", Instant.parse("2026-10-04T06:00:00Z"), resolvedAt);
    }

    @TestConfiguration
    static class StubConfiguration {
        @Bean
        @Primary
        StubJsmClient stubJsmClient() {
            return new StubJsmClient();
        }
    }

    static final class StubJsmClient implements JsmClient {
        private final ArrayDeque<JsmAlertPage> pages = new ArrayDeque<>();
        void add(JsmAlertPage page) { pages.add(page); }
        void clear() { pages.clear(); }
        @Override public JsmAlertPage fetchAlerts(Instant updatedSince, String cursor) { return pages.remove(); }
    }
}
