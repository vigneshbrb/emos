package com.emos.platform.diagnostics;

import com.emos.platform.safety.SafeContent;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class JdbcIntegrationStatusRepository implements IntegrationStatusRepository {
  private final JdbcTemplate jdbc;

  JdbcIntegrationStatusRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void recordSuccess(String provider, Instant at) {
    jdbc.update(
        """
                insert into platform_integration_status (provider, last_success_at)
                values (?, ?)
                on conflict (provider) do update set last_success_at = excluded.last_success_at
                """,
        provider,
        Timestamp.from(at));
  }

  @Override
  public void recordFailure(String provider, Instant at, String safeMessage) {
    jdbc.update(
        """
                insert into platform_integration_status (provider, last_failure_at, safe_message)
                values (?, ?, ?)
                on conflict (provider) do update
                set last_failure_at = excluded.last_failure_at, safe_message = excluded.safe_message
                """,
        provider,
        Timestamp.from(at),
        SafeContent.safeMessage(safeMessage));
  }

  @Override
  public List<IntegrationStatus> findAll() {
    return jdbc.query(
        """
                select provider, last_success_at, last_failure_at, safe_message
                from platform_integration_status order by provider
                """,
        (rs, rowNumber) ->
            new IntegrationStatus(
                rs.getString("provider"),
                rs.getTimestamp("last_success_at") == null
                    ? null
                    : rs.getTimestamp("last_success_at").toInstant(),
                rs.getTimestamp("last_failure_at") == null
                    ? null
                    : rs.getTimestamp("last_failure_at").toInstant(),
                rs.getString("safe_message")));
  }
}
