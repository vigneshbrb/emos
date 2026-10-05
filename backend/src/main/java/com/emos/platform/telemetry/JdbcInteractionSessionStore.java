package com.emos.platform.telemetry;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcInteractionSessionStore implements InteractionSessionStore {
  private final JdbcTemplate jdbc;

  public JdbcInteractionSessionStore(JdbcTemplate j) {
    jdbc = j;
  }

  public SessionRecord insert(SessionRecord r) {
    jdbc.update(
        "insert into interaction_session(id,case_id,started_at,last_heartbeat_at,last_visible_active,active_seconds) values(?,?,?,?,?,?)",
        r.id(),
        r.caseId(),
        Timestamp.from(r.startedAt()),
        Timestamp.from(r.lastHeartbeatAt()),
        r.lastVisibleActive(),
        r.activeSeconds());
    return r;
  }

  public SessionRecord get(UUID id) {
    return jdbc.query("select * from interaction_session where id=?", this::map, id).stream()
        .findFirst()
        .orElseThrow();
  }

  public boolean addHeartbeat(UUID id, Instant at, boolean visibleActive) {
    return jdbc.update(
            "insert into interaction_heartbeat(session_id,occurred_at,visible_active) values(?,?,?) on conflict do nothing",
            id,
            Timestamp.from(at),
            visibleActive)
        == 1;
  }

  public void update(SessionRecord r) {
    jdbc.update(
        "update interaction_session set last_heartbeat_at=?,last_visible_active=?,stopped_at=?,active_seconds=? where id=?",
        Timestamp.from(r.lastHeartbeatAt()),
        r.lastVisibleActive(),
        r.stoppedAt() == null ? null : Timestamp.from(r.stoppedAt()),
        r.activeSeconds(),
        r.id());
  }

  private SessionRecord map(ResultSet rs, int n) throws SQLException {
    return new SessionRecord(
        rs.getObject("id", UUID.class),
        rs.getObject("case_id", UUID.class),
        rs.getTimestamp("started_at").toInstant(),
        rs.getTimestamp("last_heartbeat_at").toInstant(),
        rs.getBoolean("last_visible_active"),
        rs.getTimestamp("stopped_at") == null ? null : rs.getTimestamp("stopped_at").toInstant(),
        rs.getLong("active_seconds"));
  }
}
