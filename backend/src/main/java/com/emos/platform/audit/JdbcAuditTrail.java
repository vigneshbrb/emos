package com.emos.platform.audit;

import com.emos.platform.safety.SafeContent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class JdbcAuditTrail implements AuditTrail, AuditQueryService {
  private final JdbcTemplate jdbc;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final RowMapper<AuditEntry> rowMapper = this::mapEntry;

  JdbcAuditTrail(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
    this.jdbc = jdbc;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  public AuditEntry append(AuditEntry entry) {
    SafeContent.requireSafe(entry.details());
    var id = entry.id() == null ? UUID.randomUUID() : entry.id();
    jdbc.update(
        """
                insert into platform_audit_entry
                    (id, subject_type, subject_id, event_type, actor_type, occurred_at,
                     details, evidence_ids, created_at)
                values (?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), ?)
                """,
        id,
        entry.subjectType(),
        entry.subjectId(),
        entry.eventType(),
        entry.actorType().name(),
        Timestamp.from(entry.occurredAt()),
        entry.details().toString(),
        objectMapper.writeValueAsString(entry.evidenceIds()),
        Timestamp.from(clock.instant()));
    return new AuditEntry(
        id,
        entry.subjectType(),
        entry.subjectId(),
        entry.eventType(),
        entry.actorType(),
        entry.occurredAt(),
        entry.details(),
        entry.evidenceIds());
  }

  @Override
  public List<AuditEntry> findForSubject(String subjectType, UUID subjectId) {
    return jdbc.query(
        """
                select * from platform_audit_entry
                where subject_type = ? and subject_id = ?
                order by occurred_at, id
                """,
        rowMapper,
        subjectType,
        subjectId);
  }

  private AuditEntry mapEntry(ResultSet rs, int rowNumber) throws SQLException {
    var evidenceIds = new ArrayList<UUID>();
    for (var node : objectMapper.readTree(rs.getString("evidence_ids"))) {
      evidenceIds.add(UUID.fromString(node.stringValue()));
    }
    return new AuditEntry(
        rs.getObject("id", UUID.class),
        rs.getString("subject_type"),
        rs.getObject("subject_id", UUID.class),
        rs.getString("event_type"),
        AuditEntry.ActorType.valueOf(rs.getString("actor_type")),
        rs.getTimestamp("occurred_at").toInstant(),
        objectMapper.readTree(rs.getString("details")),
        evidenceIds);
  }
}
