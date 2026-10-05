package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(name = "emos.jira.enabled", havingValue = "true")
public class JiraHttpIssueAdapter implements JiraIssuePort {
  private final RestClient http;
  private final JsonMapper json;
  private final String project;

  public JiraHttpIssueAdapter(
      RestClient.Builder builder,
      JsonMapper json,
      @Value("${emos.jira.base-url:https://example.atlassian.net}") String baseUrl,
      @Value("${emos.jira.user:}") String user,
      @Value("${emos.jira.token:}") String token,
      @Value("${emos.jira.project:}") String project) {
    var auth =
        Base64.getEncoder().encodeToString((user + ":" + token).getBytes(StandardCharsets.UTF_8));
    this.http = builder.baseUrl(baseUrl).defaultHeader("Authorization", "Basic " + auth).build();
    this.json = json;
    this.project = project;
  }

  public Optional<JiraIssueRef> findByCorrelationKey(String key) {
    try {
      var body =
          http.get()
              .uri(
                  uri ->
                      uri.path("/rest/api/3/search/jql")
                          .queryParam("jql", "labels = \"" + label(key) + "\"")
                          .build())
              .retrieve()
              .body(String.class);
      var issues = json.readTree(body).get("issues");
      if (issues == null || issues.isEmpty()) return Optional.empty();
      var issue = issues.get(0);
      return Optional.of(new JiraIssueRef(issue.get("key").asText(), issue.get("self").asText()));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Invalid Jira response", e);
    }
  }

  public JiraIssueRef create(ApprovedJiraDraft draft, String key) {
    try {
      var root = json.createObjectNode();
      var fields = root.putObject("fields");
      fields.putObject("project").put("key", project);
      fields.put("summary", draft.title());
      fields.putObject("issuetype").put("name", "Story");
      var description = fields.putObject("description");
      description.put("type", "doc").put("version", 1);
      description
          .putArray("content")
          .addObject()
          .put("type", "paragraph")
          .putArray("content")
          .addObject()
          .put("type", "text")
          .put(
              "text",
              draft.problemStatement()
                  + "\n\nProposed: "
                  + draft.proposedDirection()
                  + "\n\nAcceptance: "
                  + draft.acceptanceIntent()
                  + "\n\nRepository: "
                  + draft.repositoryId()
                  + "\nReview: "
                  + draft.reviewDate());
      fields.putArray("labels").add(label(key));
      var response =
          json.readTree(
              http.post().uri("/rest/api/3/issue").body(root).retrieve().body(String.class));
      return new JiraIssueRef(response.get("key").asText(), response.get("self").asText());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Invalid Jira response", e);
    }
  }

  public JiraIssueObservation observe(JiraIssueRef issue) {
    try {
      var root =
          json.readTree(
              http.get()
                  .uri("/rest/api/3/issue/{key}?fields=status", issue.key())
                  .retrieve()
                  .body(String.class));
      return new JiraIssueObservation(
          root.get("fields").get("status").get("name").asText(), true, java.time.Instant.now());
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
      return new JiraIssueObservation("Deleted", false, java.time.Instant.now());
    }
  }

  private static String label(String key) {
    return "emos-" + UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }
}
