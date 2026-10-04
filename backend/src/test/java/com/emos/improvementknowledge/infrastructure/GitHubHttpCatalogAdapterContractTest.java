package com.emos.improvementknowledge.infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubHttpCatalogAdapterContractTest {
 @RegisterExtension static WireMockExtension github = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort().http2PlainDisabled(true)).build();
 @Test void fetches_only_permitted_metadata_and_documents() {
   github.stubFor(get("/orgs/acme/repos").willReturn(okJson("[{\"name\":\"payments\",\"description\":\"Payments\",\"topics\":[\"sql\"],\"default_branch\":\"main\"}]")));
   github.stubFor(get(urlPathMatching("/repos/acme/payments/contents/.*")).willReturn(notFound()));
   var adapter = new GitHubHttpCatalogAdapter(RestClient.builder(), JsonMapper.builder().build(), github.baseUrl(), "token", "acme");
   assertThat(adapter.refresh().repositories()).hasSize(1);
   github.verify(getRequestedFor(urlEqualTo("/repos/acme/payments/contents/README.md?ref=main")));
   github.verify(getRequestedFor(urlEqualTo("/repos/acme/payments/contents/CODEOWNERS?ref=main")));
   github.verify(getRequestedFor(urlEqualTo("/repos/acme/payments/contents/.github/CODEOWNERS?ref=main")));
   github.verify(getRequestedFor(urlEqualTo("/repos/acme/payments/contents/deployment.yaml?ref=main")));
   github.verify(0, getRequestedFor(urlPathMatching(".*/src/.*")));
 }
}
