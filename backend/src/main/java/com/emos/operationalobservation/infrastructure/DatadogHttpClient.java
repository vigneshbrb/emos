package com.emos.operationalobservation.infrastructure;

import com.emos.operationalobservation.application.DatadogClient;
import com.emos.operationalobservation.application.MonitorEvidence;
import com.emos.operationalobservation.application.MonitorTransition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

@Component
@ConditionalOnProperty(name = "emos.datadog.enabled", havingValue = "true")
public class DatadogHttpClient implements DatadogClient {
    private final RestClient client;
    private final ObjectMapper mapper;

    public DatadogHttpClient(RestClient.Builder builder, ObjectMapper mapper,
                             @Value("${emos.datadog.base-url:https://api.datadoghq.com}") String baseUrl,
                             @Value("${emos.datadog.api-key:}") String apiKey,
                             @Value("${emos.datadog.application-key:}") String applicationKey) {
        this.client = builder.baseUrl(baseUrl).defaultHeaders(headers -> {
            headers.set("DD-API-KEY", apiKey);
            headers.set("DD-APPLICATION-KEY", applicationKey);
            headers.set("Accept", "application/json");
        }).build();
        this.mapper = mapper;
    }

    @Override
    public MonitorEvidence loadEvidence(String monitorId, Instant asOf) {
        var root = mapper.createObjectNode();
        var filter = root.putObject("filter");
        filter.put("query", "@monitor.id:" + monitorId);
        filter.put("from", asOf.minus(Duration.ofDays(30)).toString());
        filter.put("to", asOf.toString());
        root.put("sort", "timestamp");
        root.putObject("page").put("limit", 1000);
        var response = client.post().uri("/api/v2/events/search").contentType(MediaType.APPLICATION_JSON)
                .body(root).retrieve().body(String.class);
        var json = mapper.readTree(response);
        var transitions = new ArrayList<MonitorTransition>();
        for (var event : json.path("data")) {
            var attributes = event.path("attributes");
            var detail = attributes.path("attributes");
            var id = detail.path("evt").path("uid").stringValue(event.path("id").stringValue());
            var eventMonitorId = detail.path("monitor").path("monitor_id").asText();
            var at = Instant.ofEpochMilli(attributes.path("timestamp").longValue());
            transitions.add(new MonitorTransition(id, eventMonitorId, at,
                    state(detail.path("status").stringValue())));
        }
        return new MonitorEvidence(monitorId, null, transitions);
    }

    private static MonitorTransition.State state(String value) {
        if ("ok".equalsIgnoreCase(value)) return MonitorTransition.State.OK;
        if ("warn".equalsIgnoreCase(value) || "warning".equalsIgnoreCase(value)) {
            return MonitorTransition.State.WARNING;
        }
        return MonitorTransition.State.ALERT;
    }
}
