package com.emos.operationalobservation.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class JsmHttpClientContractTest {

  @RegisterExtension
  static WireMockExtension jsm =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Test
  void maps_official_alert_fields_and_offset_pagination_without_leaking_credentials() {
    jsm.stubFor(
        get(urlPathEqualTo("/api/cloud-1/v1/alerts"))
            .withQueryParam("size", equalTo("100"))
            .withQueryParam("offset", equalTo("10"))
            .withQueryParam("sort", equalTo("updatedAt"))
            .withQueryParam("order", equalTo("asc"))
            .withQueryParam("from", equalTo("1791093600000"))
            .withBasicAuth("manager@example.com", "configured-token")
            .willReturn(
                okJson(
                    """
                        {
                          "values": [{
                            "id": "alert-42",
                            "createdAt": "2026-10-04T06:00:00Z",
                            "updatedAt": "2026-10-04T06:20:00Z",
                            "status": "closed",
                            "closeTime": "2026-10-04T06:20:00Z",
                            "priority": "P2",
                            "description": "Check latency dashboards",
                            "extraProperties": {
                              "monitorUrl": "https://app.datadoghq.com/monitors/17",
                              "alertUrl": "https://jsm.example/alerts/alert-42"
                            }
                          }],
                          "count": 1,
                          "links": {"next": "/v1/alerts?offset=110&size=100"}
                        }
                        """)));
    var client =
        new JsmHttpClient(
            RestClient.builder(),
            JsonMapper.builder().build(),
            jsm.baseUrl(),
            "cloud-1",
            "manager@example.com",
            "configured-token");

    var page = client.fetchAlerts(Instant.parse("2026-10-04T06:00:00Z"), "10");

    assertThat(page.nextCursor()).isEqualTo("110");
    assertThat(page.alerts())
        .singleElement()
        .satisfies(
            record -> {
              assertThat(record.snapshot().sourceId()).isEqualTo("alert-42");
              assertThat(record.snapshot().status().name()).isEqualTo("RESOLVED");
              assertThat(record.snapshot().resolvedAt())
                  .isEqualTo(Instant.parse("2026-10-04T06:20:00Z"));
              assertThat(record.snapshot().monitorUrl())
                  .isEqualTo("https://app.datadoghq.com/monitors/17");
              assertThat(record.snapshot().runbook()).isEqualTo("Check latency dashboards");
              assertThat(record.snapshot().severity()).isEqualTo("P2");
              assertThat(record.rawPayload().get("id").stringValue()).isEqualTo("alert-42");
            });
    jsm.verify(1, getRequestedFor(urlPathEqualTo("/api/cloud-1/v1/alerts")));
  }
}
