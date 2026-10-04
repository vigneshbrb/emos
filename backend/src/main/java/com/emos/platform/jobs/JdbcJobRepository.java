package com.emos.platform.jobs;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcJobRepository implements JobRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RowMapper<Job> rowMapper = this::mapJob;

    JdbcJobRepository(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Job enqueue(String type, String deduplicationKey, JsonNode payload, Instant availableAt) {
        var id = UUID.randomUUID();
        return jdbc.queryForObject("""
                insert into platform_job
                    (id, type, deduplication_key, payload, state, available_at, attempts, created_at)
                values (?, ?, ?, cast(? as jsonb), 'READY', ?, 0, ?)
                on conflict (type, deduplication_key)
                do update set deduplication_key = excluded.deduplication_key
                returning *
                """, rowMapper, id, type, deduplicationKey, payload.toString(), Timestamp.from(availableAt),
                Timestamp.from(clock.instant()));
    }

    @Override
    @Transactional
    public List<Job> claimAvailable(int limit, Instant now, Duration leaseDuration) {
        return jdbc.query("""
                with candidates as (
                    select id
                    from platform_job
                    where (state = 'READY' and available_at <= ?)
                       or (state = 'RUNNING' and lease_until <= ?)
                    order by available_at, id
                    for update skip locked
                    limit ?
                )
                update platform_job job
                set state = 'RUNNING', lease_until = ?, attempts = attempts + 1
                from candidates
                where job.id = candidates.id
                returning job.*
                """, rowMapper, Timestamp.from(now), Timestamp.from(now), limit,
                Timestamp.from(now.plus(leaseDuration)));
    }

    @Override
    public void markSucceeded(UUID id, Instant completedAt) {
        jdbc.update("""
                update platform_job
                set state = 'SUCCEEDED', lease_until = null, completed_at = ?, last_error = null
                where id = ? and state = 'RUNNING'
                """, Timestamp.from(completedAt), id);
    }

    @Override
    public void markFailed(UUID id, Instant availableAt, String safeMessage) {
        jdbc.update("""
                update platform_job
                set state = 'READY', available_at = ?, lease_until = null, last_error = ?
                where id = ? and state = 'RUNNING'
                """, Timestamp.from(availableAt), safeMessage, id);
    }

    @Override
    public List<Job> findAll() {
        return jdbc.query("select * from platform_job order by created_at, id", rowMapper);
    }

    private Job mapJob(ResultSet rs, int rowNumber) throws SQLException {
        return new Job(
                rs.getObject("id", UUID.class),
                rs.getString("type"),
                rs.getString("deduplication_key"),
                objectMapper.readTree(rs.getString("payload")),
                Job.State.valueOf(rs.getString("state")),
                rs.getTimestamp("available_at").toInstant(),
                rs.getTimestamp("lease_until") == null ? null : rs.getTimestamp("lease_until").toInstant(),
                rs.getInt("attempts"),
                rs.getString("last_error")
        );
    }
}
