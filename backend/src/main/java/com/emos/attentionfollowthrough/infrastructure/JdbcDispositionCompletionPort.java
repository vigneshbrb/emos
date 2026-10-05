package com.emos.attentionfollowthrough.infrastructure;

import com.emos.attentionfollowthrough.application.DispositionCompletionPort;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JdbcDispositionCompletionPort implements DispositionCompletionPort {
  private final JdbcTemplate jdbc;

  public JdbcDispositionCompletionPort(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean hasActive(OperationalCaseId id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from disposition_obligation where subject_case_id=? and state in ('PENDING','BREACHED'))",
            Boolean.class,
            id.value()));
  }

  public boolean complete(OperationalCaseId id, Instant at) {
    var changed =
        jdbc.update(
                "update disposition_obligation set state='SATISFIED',satisfied_at=? where subject_case_id=? and state in ('PENDING','BREACHED')",
                Timestamp.from(at),
                id.value())
            == 1;
    jdbc.update(
        "update attention_item set state='RESOLVED',resolved_at=? where subject_case_id=? and state='OPEN'",
        Timestamp.from(at),
        id.value());
    return changed;
  }
}
