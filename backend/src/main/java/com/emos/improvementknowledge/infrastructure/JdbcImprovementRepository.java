package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import com.emos.improvementknowledge.domain.JiraDraft;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JdbcImprovementRepository implements ImprovementRepository {
  private final JdbcTemplate jdbc;

  public JdbcImprovementRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<JiraDraft> findDraft(UUID id, OperationalCaseId caseId) {
    return jdbc
        .query("select * from jira_draft where id=? and case_id=?", this::map, id, caseId.value())
        .stream()
        .findFirst();
  }

  public Optional<JiraDraft> findDraft(OperationalCaseId caseId) {
    return jdbc
        .query("select * from jira_draft where case_id=?", this::map, caseId.value())
        .stream()
        .findFirst();
  }

  private JiraDraft map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
    return new JiraDraft(
        rs.getObject("id", UUID.class),
        new OperationalCaseId(rs.getObject("case_id", UUID.class)),
        rs.getString("title"),
        rs.getString("problem_statement"),
        rs.getString("evidence_summary"),
        rs.getString("proposed_direction"),
        rs.getString("acceptance_intent"),
        rs.getString("repository_id"),
        rs.getObject("review_date", LocalDate.class),
        rs.getBoolean("approved"),
        rs.getTimestamp("updated_at").toInstant());
  }

  public JiraDraft saveDraft(JiraDraft d) {
    jdbc.update(
        "insert into jira_draft(id,case_id,title,problem_statement,evidence_summary,proposed_direction,acceptance_intent,repository_id,review_date,approved,updated_at) values(?,?,?,?,?,?,?,?,?,?,?) on conflict(case_id) do update set title=excluded.title,problem_statement=excluded.problem_statement,evidence_summary=excluded.evidence_summary,proposed_direction=excluded.proposed_direction,acceptance_intent=excluded.acceptance_intent,repository_id=excluded.repository_id,review_date=excluded.review_date,approved=excluded.approved,updated_at=excluded.updated_at",
        d.id(),
        d.caseId().value(),
        d.title(),
        d.problemStatement(),
        d.evidenceSummary(),
        d.proposedDirection(),
        d.acceptanceIntent(),
        d.repositoryId(),
        d.reviewDate(),
        d.approved(),
        Timestamp.from(d.updatedAt()));
    return findDraft(d.caseId()).orElseThrow();
  }

  public boolean isConfirmed(OperationalCaseId caseId, String repositoryId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "select exists(select 1 from repository_mapping_current c join repository_mapping_history h on h.id=c.history_id where h.case_id=? and c.repository_id=?)",
            Boolean.class,
            caseId.value(),
            repositoryId));
  }

  public Optional<JiraIssueRef> findAttempt(UUID id) {
    return jdbc
        .query(
            "select jira_key,jira_url from disposition_attempt where id=?",
            (rs, n) -> new JiraIssueRef(rs.getString(1), rs.getString(2)),
            id)
        .stream()
        .findFirst();
  }

  public void record(
      UUID attempt,
      OperationalCaseId caseId,
      String correlation,
      JiraIssueRef issue,
      LocalDate review,
      Instant at) {
    jdbc.update(
        "insert into disposition_attempt(id,case_id,correlation_key,jira_key,jira_url,created_at) values(?,?,?,?,?,?) on conflict(id) do nothing",
        attempt,
        caseId.value(),
        correlation,
        issue.key(),
        issue.url(),
        Timestamp.from(at));
    jdbc.update(
        "insert into improvement_follow_up(id,case_id,jira_key,jira_url,review_date,state,created_at) values(?,?,?,?,?,'OPEN',?) on conflict(jira_key) do nothing",
        UUID.randomUUID(),
        caseId.value(),
        issue.key(),
        issue.url(),
        review,
        Timestamp.from(at));
  }
}
