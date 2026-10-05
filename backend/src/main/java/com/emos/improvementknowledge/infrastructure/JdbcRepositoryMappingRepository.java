package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JdbcRepositoryMappingRepository implements RepositoryMappingRepository {
  private final JdbcTemplate jdbc;

  public JdbcRepositoryMappingRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void confirm(
      MappingId id,
      OperationalCaseId caseId,
      String monitorId,
      RepositoryRef ref,
      String rationale,
      Instant at) {
    jdbc.update(
        "insert into repository_mapping_history(id,case_id,monitor_id,repository_id,rationale,confirmed_at) values(?,?,?,?,?,?)",
        id.value(),
        caseId.value(),
        monitorId,
        ref.value(),
        rationale,
        Timestamp.from(at));
    jdbc.update(
        "insert into repository_mapping_current(monitor_id,history_id,repository_id,rationale,confirmed_at) values(?,?,?,?,?) on conflict(monitor_id) do update set history_id=excluded.history_id,repository_id=excluded.repository_id,rationale=excluded.rationale,confirmed_at=excluded.confirmed_at",
        monitorId,
        id.value(),
        ref.value(),
        rationale,
        Timestamp.from(at));
  }

  public Optional<RepositoryMapping> findCurrent(String monitorId) {
    return jdbc
        .query(
            "select monitor_id,repository_id,rationale from repository_mapping_current where monitor_id=?",
            (rs, n) ->
                new RepositoryMapping(rs.getString(1), rs.getString(2), rs.getString(3), true),
            monitorId)
        .stream()
        .findFirst();
  }
}
