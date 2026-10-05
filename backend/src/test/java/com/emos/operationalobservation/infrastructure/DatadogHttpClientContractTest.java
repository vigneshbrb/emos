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

class DatadogHttpClientContractTest {
  @RegisterExtension
  static WireMockExtension datadog =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  @Test
  void searches_exact_monitor_30_day_history_and_maps_transition_identity() {
    datadog.stubFor(
        post(urlPathEqualTo("/api/v2/events/search"))
            .withHeader("DD-API-KEY", equalTo("api-secret"))
            .withHeader("DD-APPLICATION-KEY", equalTo("app-secret"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(matchingJsonPath("$.filter.query", equalTo("@monitor.id:17")))
            .withRequestBody(matchingJsonPath("$.filter.from", equalTo("2026-09-04T12:00:00Z")))
            .withRequestBody(matchingJsonPath("$.filter.to", equalTo("2026-10-04T12:00:00Z")))
            .willReturn(
                okJson(
                    """
                        {"data":[{"id":"event-a","attributes":{"timestamp":1791109500000,
                          "attributes":{"monitor":{"monitor_id":17},"status":"alert","evt":{"uid":"occ-a"}}}}],
                         "meta":{"page":{}}}
                        """)));
    var client =
        new DatadogHttpClient(
            RestClient.builder(),
            JsonMapper.builder().build(),
            datadog.baseUrl(),
            "api-secret",
            "app-secret");

    var result = client.loadEvidence("17", Instant.parse("2026-10-04T12:00:00Z"));

    assertThat(result.monitorId()).isEqualTo("17");
    assertThat(result.transitions())
        .singleElement()
        .satisfies(
            transition -> {
              assertThat(transition.providerOccurrenceId()).isEqualTo("occ-a");
              assertThat(transition.monitorId()).isEqualTo("17");
              assertThat(transition.occurredAt()).isEqualTo(Instant.parse("2026-10-04T10:25:00Z"));
              assertThat(transition.state())
                  .isEqualTo(
                      com.emos.operationalobservation.application.MonitorTransition.State.ALERT);
            });
  }
}
