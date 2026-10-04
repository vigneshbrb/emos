package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.EvidenceQueryService;
import com.emos.operationalobservation.application.RecurrenceCounts;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcEvidenceQueryService implements EvidenceQueryService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    JdbcEvidenceQueryService(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    @Override public Optional<EvidenceSummary> findLatest(OperationalCaseId caseId) {
        return jdbc.query("""
                select * from operational_evidence_snapshot where case_id=? order by observed_at desc limit 1
                """, (rs, row) -> new EvidenceSummary(rs.getTimestamp("observed_at").toInstant(),
                rs.getString("monitor_id"), rs.getString("jsm_severity"),
                rs.getObject("latest_duration_seconds", Long.class),
                new RecurrenceCounts(rs.getInt("recurrence_24h"), rs.getInt("recurrence_7d"), rs.getInt("recurrence_30d")),
                strings(rs.getString("provider_occurrence_ids")), strings(rs.getString("source_links"))),
                caseId.value()).stream().findFirst();
    }

    private List<String> strings(String json) {
        return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
