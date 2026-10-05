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
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos-today-api",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class TodayControllerIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute(
        "truncate table attention_item, disposition_obligation, operational_evidence_snapshot, operational_alert, operational_source_event, platform_audit_entry");
  }

  @Test
  void returns_attention_pending_and_active_signals_with_attention_longest_overdue_first()
      throws Exception {
    var attentionOlder =
        insertAlert("attention-old", "P2", "RESOLVED", Instant.parse("2026-10-01T00:00:00Z"));
    var attentionNewer =
        insertAlert("attention-new", "P1", "RESOLVED", Instant.parse("2026-10-02T00:00:00Z"));
    var pending = insertAlert("pending", "P2", "RESOLVED", Instant.parse("2026-10-03T00:00:00Z"));
    var active = insertAlert("active", "P1", "ACTIVE", Instant.parse("2026-10-04T00:00:00Z"));
    insertObligation(attentionOlder, "BREACHED", Instant.parse("2026-10-02T00:00:00Z"), true);
    insertObligation(attentionNewer, "BREACHED", Instant.parse("2026-10-03T00:00:00Z"), true);
    insertObligation(pending, "PENDING", Instant.parse("2026-10-06T00:00:00Z"), false);

    mvc.perform(get("/api/today"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attentionNow.length()").value(2))
        .andExpect(jsonPath("$.attentionNow[0].caseId").value(attentionOlder.toString()))
        .andExpect(jsonPath("$.attentionNow[0].confirmedIncident").value(false))
        .andExpect(jsonPath("$.pending[0].caseId").value(pending.toString()))
        .andExpect(jsonPath("$.pending[0].deadline").value("2026-10-06T00:00:00Z"))
        .andExpect(jsonPath("$.activeSignals[0].caseId").value(active.toString()))
        .andExpect(jsonPath("$.activeSignals[0].attention").doesNotExist());
  }

  private UUID insertAlert(String source, String severity, String status, Instant updated) {
    var id = UUID.randomUUID();
    jdbc.update(
        """
                insert into operational_alert(case_id,source_id,status,source_url,monitor_url,runbook,severity,
                  triggered_at,resolved_at,source_updated_at,domain_version) values (?,?,?,?,?,?,?,?,?,?,0)
                """,
        id,
        source,
        status,
        "https://jsm/" + source,
        "https://app.datadoghq.com/monitors/17",
        "Runbook",
        severity,
        Timestamp.from(updated.minusSeconds(600)),
        "RESOLVED".equals(status) ? Timestamp.from(updated) : null,
        Timestamp.from(updated));
    return id;
  }

  private void insertObligation(UUID caseId, String state, Instant deadline, boolean attention) {
    var id = UUID.randomUUID();
    jdbc.update(
        """
                insert into disposition_obligation(id,expectation_key,policy_version,subject_case_id,
                  source_resolved_at,deadline,state,breached_at) values (?,'operational-case-disposition',1,?,?,?,?,?)
                """,
        id,
        caseId,
        Timestamp.from(deadline.minusSeconds(86_400)),
        Timestamp.from(deadline),
        state,
        attention ? Timestamp.from(deadline) : null);
    if (attention)
      jdbc.update(
          """
                insert into attention_item(id,obligation_id,subject_case_id,reason,state,opened_at)
                values (?,?,?,'Improvement decision missing','OPEN',?)
                """,
          UUID.randomUUID(),
          id,
          caseId,
          Timestamp.from(deadline));
  }
}
