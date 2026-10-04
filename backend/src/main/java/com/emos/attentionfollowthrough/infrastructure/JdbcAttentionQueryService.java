package com.emos.attentionfollowthrough.infrastructure;

import com.emos.attentionfollowthrough.application.AttentionQueryService;
import com.emos.attentionfollowthrough.domain.ObligationId;
import com.emos.attentionfollowthrough.domain.ObligationState;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcAttentionQueryService implements AttentionQueryService {
    private final JdbcTemplate jdbc;
    JdbcAttentionQueryService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public List<AttentionSummary> findOpen() {
        return jdbc.query("""
                select a.*, o.deadline from attention_item a join disposition_obligation o on o.id=a.obligation_id
                where a.state='OPEN' order by o.deadline, a.id
                """, (rs, row) ->
                new AttentionSummary(rs.getObject("id", UUID.class),
                        new ObligationId(rs.getObject("obligation_id", UUID.class)),
                        new OperationalCaseId(rs.getObject("subject_case_id", UUID.class)),
                        rs.getString("reason"), rs.getTimestamp("deadline").toInstant(),
                        rs.getTimestamp("opened_at").toInstant()));
    }

    @Override public List<PendingObligationSummary> findPending() {
        return jdbc.query("select * from disposition_obligation where state='PENDING' order by deadline, id", (rs, row) ->
                new PendingObligationSummary(new ObligationId(rs.getObject("id", UUID.class)),
                        new OperationalCaseId(rs.getObject("subject_case_id", UUID.class)),
                        ObligationState.valueOf(rs.getString("state")), rs.getTimestamp("deadline").toInstant()));
    }
}
