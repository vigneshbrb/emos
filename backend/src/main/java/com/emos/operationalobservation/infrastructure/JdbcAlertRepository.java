package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.AlertRepository;
import com.emos.operationalobservation.application.JsmAlertSnapshot;
import com.emos.operationalobservation.domain.Alert;
import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcAlertRepository implements AlertRepository {
    private final JdbcTemplate jdbc;

    JdbcAlertRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Alert> findBySourceId(String sourceId) {
        return jdbc.query("select * from operational_alert where source_id = ?", this::map, sourceId)
                .stream().findFirst();
    }

    @Override
    public Optional<Alert> findByCaseId(OperationalCaseId caseId) {
        return jdbc.query("select * from operational_alert where case_id = ?", this::map, caseId.value())
                .stream().findFirst();
    }

    @Override
    public void insert(Alert alert) {
        jdbc.update("""
                insert into operational_alert
                    (case_id, source_id, status, source_url, monitor_url, runbook, severity,
                     triggered_at, resolved_at, source_updated_at, domain_version)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, alert.caseId().value(), alert.sourceId(), alert.status().name(), alert.sourceUrl(),
                alert.monitorUrl(), alert.runbook(), alert.severity(), timestamp(alert.triggeredAt()),
                timestamp(alert.resolvedAt()), timestamp(alert.lastSourceUpdatedAt()), alert.version());
    }

    @Override
    public void update(Alert alert) {
        var rows = jdbc.update("""
                update operational_alert
                set status = ?, source_url = ?, monitor_url = ?, runbook = ?, severity = ?,
                    triggered_at = ?, resolved_at = ?, source_updated_at = ?, domain_version = ?
                where source_id = ? and domain_version = ?
                """, alert.status().name(), alert.sourceUrl(), alert.monitorUrl(), alert.runbook(), alert.severity(),
                timestamp(alert.triggeredAt()), timestamp(alert.resolvedAt()), timestamp(alert.lastSourceUpdatedAt()),
                alert.version(), alert.sourceId(), alert.version() - 1);
        if (rows != 1) throw new OptimisticLockingFailureException("Alert was concurrently updated");
    }

    private Alert map(ResultSet rs, int rowNumber) throws SQLException {
        var status = AlertStatus.valueOf(rs.getString("status"));
        var snapshot = new JsmAlertSnapshot(
                rs.getString("source_id"),
                rs.getTimestamp("source_updated_at").toInstant(),
                status,
                rs.getString("source_url"),
                rs.getString("monitor_url"),
                rs.getString("runbook"),
                rs.getString("severity"),
                instant(rs.getTimestamp("triggered_at")),
                instant(rs.getTimestamp("resolved_at")));
        return Alert.restore(new OperationalCaseId(rs.getObject("case_id", UUID.class)), snapshot,
                rs.getLong("domain_version"));
    }

    private static Timestamp timestamp(java.time.Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static java.time.Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
