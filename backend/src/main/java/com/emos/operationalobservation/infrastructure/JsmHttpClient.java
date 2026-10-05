package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.JsmAlertPage;
import com.emos.operationalobservation.application.JsmAlertRecord;
import com.emos.operationalobservation.application.JsmAlertSnapshot;
import com.emos.operationalobservation.application.JsmClient;
import com.emos.operationalobservation.domain.AlertStatus;
import java.time.Instant;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "emos.jsm.enabled", havingValue = "true")
public class JsmHttpClient implements JsmClient {
  private static final int PAGE_SIZE = 100;

  private final RestClient client;
  private final ObjectMapper objectMapper;
  private final String cloudId;

  public JsmHttpClient(
      RestClient.Builder builder,
      ObjectMapper objectMapper,
      @Value("${emos.jsm.base-url:https://api.atlassian.com/jsm/ops}") String baseUrl,
      @Value("${emos.jsm.cloud-id:}") String cloudId,
      @Value("${emos.jsm.email:}") String email,
      @Value("${emos.jsm.token:}") String token) {
    this.client =
        builder
            .baseUrl(baseUrl)
            .defaultHeaders(
                headers -> {
                  headers.setBasicAuth(email, token);
                  headers.set("Accept", "application/json");
                })
            .build();
    this.objectMapper = objectMapper;
    this.cloudId = cloudId;
  }

  @Override
  public JsmAlertPage fetchAlerts(Instant updatedSince, String cursor) {
    var offset = cursor == null ? "0" : cursor;
    var body =
        client
            .get()
            .uri(
                uri ->
                    uri.path("/api/{cloudId}/v1/alerts")
                        .queryParam("size", PAGE_SIZE)
                        .queryParam("offset", offset)
                        .queryParam("sort", "updatedAt")
                        .queryParam("order", "asc")
                        .queryParam("from", updatedSince.toEpochMilli())
                        .build(cloudId))
            .retrieve()
            .body(String.class);
    var root = objectMapper.readTree(body);
    var alerts = new ArrayList<JsmAlertRecord>();
    for (var raw : root.path("values")) {
      alerts.add(new JsmAlertRecord(toSnapshot(raw), raw.deepCopy()));
    }
    return new JsmAlertPage(alerts, nextOffset(root.path("links").path("next").stringValue(null)));
  }

  private JsmAlertSnapshot toSnapshot(JsonNode raw) {
    var updatedAt = Instant.parse(requiredText(raw, "updatedAt"));
    var closed = "closed".equalsIgnoreCase(requiredText(raw, "status"));
    var extra = raw.path("extraProperties");
    var resolvedAt =
        closed
            ? Instant.parse(raw.path("closeTime").stringValue(requiredText(raw, "updatedAt")))
            : null;
    return new JsmAlertSnapshot(
        requiredText(raw, "id"),
        updatedAt,
        closed ? AlertStatus.RESOLVED : AlertStatus.ACTIVE,
        extra.path("alertUrl").stringValue(null),
        extra.path("monitorUrl").stringValue(null),
        raw.path("description").stringValue(null),
        raw.path("priority").stringValue(null),
        Instant.parse(requiredText(raw, "createdAt")),
        resolvedAt);
  }

  private String nextOffset(String nextLink) {
    if (nextLink == null || nextLink.isBlank()) return null;
    return UriComponentsBuilder.fromUriString(nextLink).build().getQueryParams().getFirst("offset");
  }

  private String requiredText(JsonNode node, String field) {
    var value = node.path(field).stringValue(null);
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("JSM field is required: " + field);
    return value;
  }
}
