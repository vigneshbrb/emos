package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.JsmPollStateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
@ConditionalOnProperty(name = "emos.persistence.enabled", havingValue = "true", matchIfMissing = true)
class JdbcJsmPollStateRepository implements JsmPollStateRepository {
    private final JdbcTemplate jdbc;

    JdbcJsmPollStateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PollState current() {
        return jdbc.queryForObject("select cursor, updated_since from operational_jsm_poll_state where singleton = true",
                (rs, rowNumber) -> new PollState(rs.getString("cursor"),
                        rs.getTimestamp("updated_since").toInstant()));
    }

    @Override
    public void advance(String cursor, Instant updatedSince) {
        jdbc.update("update operational_jsm_poll_state set cursor = ?, updated_since = ? where singleton = true",
                cursor, Timestamp.from(updatedSince));
    }
}
