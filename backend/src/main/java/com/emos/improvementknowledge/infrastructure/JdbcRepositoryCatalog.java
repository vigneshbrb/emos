package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class JdbcRepositoryCatalog implements RepositoryCatalogStore {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public JdbcRepositoryCatalog(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public List<RepositoryDocument> findFor(OperationalCaseId ignored) {
    return jdbc.query(
        "select * from repository_catalog",
        (rs, n) ->
            new RepositoryDocument(
                rs.getString("repository_id"),
                rs.getString("description"),
                new LinkedHashSet<>(strings(rs.getString("topics"))),
                strings(rs.getString("permitted_documents")),
                rs.getString("revision"),
                rs.getTimestamp("retrieved_at").toInstant(),
                rs.getBoolean("accessible")));
  }

  private List<String> strings(String value) {
    return json.readValue(
        value, json.getTypeFactory().constructCollectionType(List.class, String.class));
  }

  public void replaceAll(List<RepositoryDocument> documents) {
    for (var d : documents)
      jdbc.update(
          "insert into repository_catalog(repository_id,description,topics,permitted_documents,revision,retrieved_at,accessible) values(?,?,?::jsonb,?::jsonb,?,?,?) on conflict(repository_id) do update set description=excluded.description,topics=excluded.topics,permitted_documents=excluded.permitted_documents,revision=excluded.revision,retrieved_at=excluded.retrieved_at,accessible=excluded.accessible",
          d.repositoryId(),
          d.description(),
          json.writeValueAsString(d.topics()),
          json.writeValueAsString(d.permittedDocuments()),
          d.revision(),
          java.sql.Timestamp.from(d.retrievedAt()),
          d.accessible());
  }
}
