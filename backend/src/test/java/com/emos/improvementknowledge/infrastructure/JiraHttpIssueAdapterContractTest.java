package com.emos.improvementknowledge.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

import com.emos.improvementknowledge.application.*;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class JiraHttpIssueAdapterContractTest {
  @RegisterExtension
  static WireMockExtension jira =
      WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort().http2PlainDisabled(true))
          .build();

  @Test
  void uncertain_create_is_reconciled_by_correlation_key_before_any_second_write() {
    jira.stubFor(
        get(urlPathEqualTo("/rest/api/3/search/jql"))
            .inScenario("reconcile")
            .whenScenarioStateIs("Started")
            .willReturn(okJson("{\"issues\":[]}"))
            .willSetStateTo("created"));
    jira.stubFor(
        post("/rest/api/3/issue")
            .withRequestBody(matchingJsonPath("$.fields.description.type", equalTo("doc")))
            .withRequestBody(matchingJsonPath("$.fields.description.version", equalTo("1")))
            .willReturn(
                aResponse()
                    .withFault(
                        com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));
    jira.stubFor(
        get(urlPathEqualTo("/rest/api/3/search/jql"))
            .inScenario("reconcile")
            .whenScenarioStateIs("created")
            .willReturn(
                okJson("{\"issues\":[{\"key\":\"OPS-42\",\"self\":\"https://jira/OPS-42\"}]}")));
    var adapter =
        new JiraHttpIssueAdapter(
            RestClient.builder(),
            JsonMapper.builder().build(),
            jira.baseUrl(),
            "user",
            "token",
            "OPS");
    var draft =
        new ApprovedJiraDraft(
            "Latency",
            "Investigate recurring latency",
            "Profile SQL",
            "Latency remains below threshold",
            "payments",
            LocalDate.parse("2026-10-11"));
    assertThat(adapter.findByCorrelationKey("case-1:attempt-1")).isEmpty();
    assertThatThrownBy(() -> adapter.create(draft, "case-1:attempt-1"))
        .isInstanceOf(RuntimeException.class);
    assertThat(adapter.findByCorrelationKey("case-1:attempt-1"))
        .get()
        .extracting(JiraIssueRef::key)
        .isEqualTo("OPS-42");
    jira.verify(1, postRequestedFor(urlEqualTo("/rest/api/3/issue")));
  }
}
