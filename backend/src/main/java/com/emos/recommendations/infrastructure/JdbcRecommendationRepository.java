package com.emos.recommendations.infrastructure;

import com.emos.operationalobservation.domain.OperationalCaseId;
import com.emos.recommendations.application.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcRecommendationRepository implements RecommendationRepository {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper;
    JdbcRecommendationRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc; this.mapper=mapper; }
    @Override public Optional<RecommendationRecord> findByKey(String key) { return one("select * from recommendation where deduplication_key=?", key); }
    @Override public Optional<RecommendationRecord> findById(UUID id) { return one("select * from recommendation where id=?", id); }
    @Override public Optional<RecommendationRecord> findLatest(OperationalCaseId id) {
        return jdbc.query("select * from recommendation where case_id=? order by requested_at desc limit 1", this::map, id.value()).stream().findFirst();
    }
    @Override public RecommendationRecord insert(RecommendationRecord r) {
        jdbc.update("""
          insert into recommendation(id,deduplication_key,case_id,evidence_version,prompt_version,status,
            structured_output,requested_at,generated_at,failure_message) values (?,?,?,?,?,?,?::jsonb,?,?,?)
          """, r.id(), r.deduplicationKey(), r.caseId().value(), r.evidenceVersion(), r.promptVersion(),
                r.status().name(), json(r.result()), Timestamp.from(r.requestedAt()), timestamp(r.generatedAt()), r.failureMessage()); return r;
    }
    @Override public void replace(RecommendationRecord r) {
        jdbc.update("update recommendation set status=?,structured_output=?::jsonb,generated_at=?,failure_message=? where id=?",
                r.status().name(), json(r.result()), timestamp(r.generatedAt()), r.failureMessage(), r.id());
    }
    private Optional<RecommendationRecord> one(String sql, Object value) { return jdbc.query(sql, this::map, value).stream().findFirst(); }
    private RecommendationRecord map(ResultSet rs, int row) throws SQLException {
        var raw = rs.getString("structured_output");
        return new RecommendationRecord(rs.getObject("id", UUID.class), rs.getString("deduplication_key"),
                new OperationalCaseId(rs.getObject("case_id", UUID.class)), rs.getString("evidence_version"),
                rs.getString("prompt_version"), RecommendationStatus.valueOf(rs.getString("status")),
                raw == null ? null : mapper.readValue(raw, RecommendationResult.class),
                rs.getTimestamp("requested_at").toInstant(), instant(rs.getTimestamp("generated_at")),
                rs.getString("failure_message"));
    }
    private String json(RecommendationResult result) { return result == null ? null : mapper.writeValueAsString(result); }
    private static Timestamp timestamp(java.time.Instant value) { return value == null ? null : Timestamp.from(value); }
    private static java.time.Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
