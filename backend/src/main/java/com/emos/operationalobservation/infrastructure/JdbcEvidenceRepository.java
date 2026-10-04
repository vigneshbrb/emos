package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.EvidenceRepository;
import com.emos.operationalobservation.application.EvidenceSnapshot;
import com.emos.operationalobservation.application.RecurrenceCounts;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcEvidenceRepository implements EvidenceRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    JdbcEvidenceRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public EvidenceSnapshot saveIfAbsent(EvidenceSnapshot snapshot) {
        jdbc.update("""
                insert into operational_evidence_snapshot
                  (id, snapshot_key, case_id, observed_at, monitor_id, jsm_severity, latest_duration_seconds,
                   recurrence_24h, recurrence_7d, recurrence_30d, provider_occurrence_ids, source_links)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                on conflict (snapshot_key) do nothing
                """, snapshot.id(), snapshot.snapshotKey(), snapshot.caseId().value(), snapshot.observedAt(),
                snapshot.monitorId(), snapshot.jsmSeverity(),
                snapshot.latestDuration() == null ? null : snapshot.latestDuration().toSeconds(),
                snapshot.recurrence().last24Hours(), snapshot.recurrence().last7Days(),
                snapshot.recurrence().last30Days(), json(snapshot.providerOccurrenceIds()), json(snapshot.sourceLinks()));
        return jdbc.query("select * from operational_evidence_snapshot where snapshot_key = ?", this::map,
                snapshot.snapshotKey()).getFirst();
    }

    private EvidenceSnapshot map(ResultSet rs, int row) throws SQLException {
        var seconds = rs.getObject("latest_duration_seconds", Long.class);
        return new EvidenceSnapshot(rs.getObject("id", UUID.class), rs.getString("snapshot_key"),
                new OperationalCaseId(rs.getObject("case_id", UUID.class)), rs.getTimestamp("observed_at").toInstant(),
                rs.getString("monitor_id"), rs.getString("jsm_severity"),
                seconds == null ? null : Duration.ofSeconds(seconds),
                new RecurrenceCounts(rs.getInt("recurrence_24h"), rs.getInt("recurrence_7d"),
                        rs.getInt("recurrence_30d")),
                strings(rs.getString("provider_occurrence_ids")), strings(rs.getString("source_links")));
    }

    private String json(List<String> values) {
        return mapper.writeValueAsString(values);
    }

    private List<String> strings(String json) {
        return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
