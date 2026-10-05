package com.emos.platform.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos-audit",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class AuditTrailIntegrationTest {

  @Autowired AuditTrail auditTrail;
  @Autowired AuditQueryService queries;
  @Autowired ObjectMapper objectMapper;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void cleanDatabase() {
    jdbc.execute("truncate table platform_audit_entry");
  }

  @Test
  void entries_preserve_occurrence_order_actor_and_subject() {
    var subjectId = UUID.randomUUID();
    var otherSubjectId = UUID.randomUUID();
    var evidenceId = UUID.randomUUID();
    var later = Instant.parse("2026-10-04T08:00:00Z");
    var earlier = Instant.parse("2026-10-04T07:00:00Z");

    auditTrail.append(
        entry(subjectId, "manager.decided", AuditEntry.ActorType.MANAGER, later, List.of()));
    auditTrail.append(
        entry(
            subjectId,
            "source.observed",
            AuditEntry.ActorType.SOURCE,
            earlier,
            List.of(evidenceId)));
    auditTrail.append(
        entry(
            subjectId, "ai.recommended", AuditEntry.ActorType.AI, later.plusSeconds(1), List.of()));
    auditTrail.append(
        entry(
            subjectId,
            "system.followed-up",
            AuditEntry.ActorType.SYSTEM,
            later.plusSeconds(2),
            List.of()));
    auditTrail.append(
        entry(otherSubjectId, "source.observed", AuditEntry.ActorType.SOURCE, earlier, List.of()));

    assertThat(queries.findForSubject("ALERT", subjectId))
        .extracting(AuditEntry::eventType)
        .containsExactly(
            "source.observed", "manager.decided", "ai.recommended", "system.followed-up");
    assertThat(queries.findForSubject("ALERT", subjectId))
        .extracting(AuditEntry::actorType)
        .containsExactly(
            AuditEntry.ActorType.SOURCE,
            AuditEntry.ActorType.MANAGER,
            AuditEntry.ActorType.AI,
            AuditEntry.ActorType.SYSTEM);
    assertThat(queries.findForSubject("ALERT", subjectId).getFirst().evidenceIds())
        .containsExactly(evidenceId);
  }

  @Test
  void persistence_rejects_updates_and_deletes() {
    var saved =
        auditTrail.append(
            entry(
                UUID.randomUUID(),
                "source.observed",
                AuditEntry.ActorType.SOURCE,
                Instant.parse("2026-10-04T07:00:00Z"),
                List.of()));

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update platform_audit_entry set event_type = 'changed' where id = ?",
                    saved.id()))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(
            () -> jdbc.update("delete from platform_audit_entry where id = ?", saved.id()))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void secret_details_are_rejected_before_persistence() {
    var details = objectMapper.createObjectNode().put("Authorization", "Bearer secret-token-value");
    var unsafe =
        new AuditEntry(
            null,
            "ALERT",
            UUID.randomUUID(),
            "source.observed",
            AuditEntry.ActorType.SOURCE,
            Instant.now(),
            details,
            List.of());

    assertThatThrownBy(() -> auditTrail.append(unsafe))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unsafe");
    assertThat(jdbc.queryForObject("select count(*) from platform_audit_entry", Integer.class))
        .isZero();
  }

  private AuditEntry entry(
      UUID subjectId,
      String eventType,
      AuditEntry.ActorType actor,
      Instant occurredAt,
      List<UUID> evidenceIds) {
    return new AuditEntry(
        null,
        "ALERT",
        subjectId,
        eventType,
        actor,
        occurredAt,
        objectMapper.createObjectNode().put("summary", "safe summary"),
        evidenceIds);
  }
}
