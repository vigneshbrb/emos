package com.emos.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos-case-api",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class OperationalCaseControllerIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute(
        "truncate table attention_item, disposition_obligation, operational_evidence_snapshot, operational_alert, platform_audit_entry");
  }

  @Test
  void returns_lifecycle_evidence_sources_actions_and_actor_aware_timeline() throws Exception {
    var id = UUID.randomUUID();
    var observed = Instant.parse("2026-10-04T12:00:00Z");
    jdbc.update(
        """
                insert into operational_alert(case_id,source_id,status,source_url,monitor_url,runbook,severity,
                  triggered_at,resolved_at,source_updated_at,domain_version) values (?,?,?,?,?,?,?,?,?,?,0)
                """,
        id,
        "alert-42",
        "RESOLVED",
        "https://jsm/42",
        "https://app.datadoghq.com/monitors/17",
        "Inspect latency",
        "P2",
        Timestamp.from(observed.minusSeconds(1800)),
        Timestamp.from(observed),
        Timestamp.from(observed));
    jdbc.update(
        """
                insert into operational_evidence_snapshot(id,snapshot_key,case_id,observed_at,monitor_id,jsm_severity,
                  latest_duration_seconds,recurrence_24h,recurrence_7d,recurrence_30d,provider_occurrence_ids,source_links)
                values (?,?,?,?,?,?,600,3,7,12,'["occ-1"]'::jsonb,'["https://datadog/17"]'::jsonb)
                """,
        UUID.randomUUID(),
        "snapshot-1",
        id,
        Timestamp.from(observed),
        "17",
        "P2");
    jdbc.update(
        """
                insert into platform_audit_entry(id,subject_type,subject_id,event_type,actor_type,occurred_at,details,evidence_ids,created_at)
                values (?,'ALERT',?,'alert.resolved','SOURCE',?,'{}'::jsonb,'[]'::jsonb,?)
                """,
        UUID.randomUUID(),
        id,
        Timestamp.from(observed),
        Timestamp.from(observed));

    mvc.perform(get("/api/cases/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lifecycle.status").value("RESOLVED"))
        .andExpect(jsonPath("$.lifecycle.runbook").value("Inspect latency"))
        .andExpect(jsonPath("$.evidence.durationSeconds").value(600))
        .andExpect(jsonPath("$.evidence.severity").value("P2"))
        .andExpect(jsonPath("$.evidence.recurrence.24h").value(3))
        .andExpect(jsonPath("$.evidence.recurrence.7d").value(7))
        .andExpect(jsonPath("$.evidence.recurrence.30d").value(12))
        .andExpect(jsonPath("$.evidence.freshness.observedAt").value(observed.toString()))
        .andExpect(jsonPath("$.sourceLinks[0]").value("https://jsm/42"))
        .andExpect(jsonPath("$.availableActions[0]").value("RECORD_DISPOSITION"))
        .andExpect(jsonPath("$.timeline[0].actorType").value("SOURCE"));
  }

  @Test
  void missing_case_returns_problem_details() throws Exception {
    mvc.perform(get("/api/cases/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.status").value(404));
  }
}
