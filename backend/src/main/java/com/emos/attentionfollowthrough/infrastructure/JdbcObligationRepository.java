package com.emos.attentionfollowthrough.infrastructure;

import com.emos.attentionfollowthrough.application.ObligationRepository;
import com.emos.attentionfollowthrough.domain.*;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class JdbcObligationRepository implements ObligationRepository {
  private final JdbcTemplate jdbc;

  JdbcObligationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Obligation> findForResolution(OperationalCaseId caseId, Instant resolvedAt) {
    return jdbc
        .query(
            "select * from disposition_obligation where subject_case_id=? and source_resolved_at=?",
            this::map,
            caseId.value(),
            Timestamp.from(resolvedAt))
        .stream()
        .findFirst();
  }

  @Override
  public Obligation insert(Obligation o) {
    jdbc.update(
        """
                insert into disposition_obligation
                  (id, expectation_key, policy_version, subject_case_id, source_resolved_at, deadline, state)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
        o.id().value(),
        o.expectationId().value(),
        o.policyVersion(),
        o.caseId().value(),
        Timestamp.from(o.sourceResolvedAt()),
        Timestamp.from(o.deadline()),
        o.state().name());
    return o;
  }

  @Override
  public void cancelPending(OperationalCaseId caseId, Instant at) {
    jdbc.update(
        "update disposition_obligation set state='CANCELLED', cancelled_at=? where subject_case_id=? and state='PENDING'",
        Timestamp.from(at),
        caseId.value());
  }

  @Override
  public List<Obligation> findPendingDue(Instant now) {
    return jdbc.query(
        "select * from disposition_obligation where state='PENDING' and deadline <= ? order by deadline for update skip locked",
        this::map,
        Timestamp.from(now));
  }

  @Override
  public boolean markBreached(ObligationId id, Instant at) {
    return jdbc.update(
            "update disposition_obligation set state='BREACHED', breached_at=? where id=? and state='PENDING'",
            Timestamp.from(at),
            id.value())
        == 1;
  }

  private Obligation map(ResultSet rs, int row) throws SQLException {
    return new Obligation(
        new ObligationId(rs.getObject("id", UUID.class)),
        new ExpectationId(rs.getString("expectation_key")),
        rs.getInt("policy_version"),
        new OperationalCaseId(rs.getObject("subject_case_id", UUID.class)),
        rs.getTimestamp("source_resolved_at").toInstant(),
        rs.getTimestamp("deadline").toInstant(),
        ObligationState.valueOf(rs.getString("state")),
        instant(rs, "breached_at"),
        instant(rs, "cancelled_at"));
  }

  private static Instant instant(ResultSet rs, String name) throws SQLException {
    var value = rs.getTimestamp(name);
    return value == null ? null : value.toInstant();
  }
}
