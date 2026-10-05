package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.SourceEventRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class JdbcSourceEventRepository implements SourceEventRepository {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  JdbcSourceEventRepository(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public Optional<UUID> insert(
      String sourceSystem, String sourceId, Instant sourceUpdatedAt, JsonNode rawPayload) {
    var id = UUID.randomUUID();
    var ids =
        jdbc.query(
            """
                insert into operational_source_event
                    (id, source_system, source_id, source_updated_at, ingested_at, raw_payload, normalization_status)
                values (?, ?, ?, ?, ?, cast(? as jsonb), 'RECEIVED')
                on conflict (source_system, source_id, source_updated_at) do nothing
                returning id
                """,
            (rs, rowNumber) -> rs.getObject("id", UUID.class),
            id,
            sourceSystem,
            sourceId,
            Timestamp.from(sourceUpdatedAt),
            Timestamp.from(clock.instant()),
            rawPayload.toString());
    return ids.stream().findFirst();
  }

  @Override
  public void markApplied(UUID id) {
    mark(id, "APPLIED");
  }

  @Override
  public void markIgnored(UUID id) {
    mark(id, "IGNORED");
  }

  private void mark(UUID id, String status) {
    jdbc.update(
        "update operational_source_event set normalization_status = ? where id = ?", status, id);
  }
}
