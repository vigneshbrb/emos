package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.AlertQueryService;
import com.emos.operationalobservation.domain.AlertStatus;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcAlertQueryService implements AlertQueryService {
    private final JdbcTemplate jdbc;

    JdbcAlertQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AlertSummary> findActive() {
        return jdbc.query("""
                select case_id, source_id, status, severity, source_url, source_updated_at
                from operational_alert where status = 'ACTIVE' order by source_updated_at desc, source_id
                """, (rs, rowNumber) -> new AlertSummary(
                new OperationalCaseId(rs.getObject("case_id", UUID.class)),
                rs.getString("source_id"),
                AlertStatus.valueOf(rs.getString("status")),
                rs.getString("severity"),
                rs.getString("source_url"),
                rs.getTimestamp("source_updated_at").toInstant()));
    }
}
