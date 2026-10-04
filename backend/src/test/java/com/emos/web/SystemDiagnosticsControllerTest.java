package com.emos.web;

import com.emos.platform.diagnostics.IntegrationStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:17:///emos-diagnostics",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
})
class SystemDiagnosticsControllerTest {

    @Autowired IntegrationStatusRepository statuses;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.update("delete from platform_integration_status");
    }

    @Test
    void integrations_returns_times_and_only_safe_failure_information() throws Exception {
        var successAt = Instant.parse("2026-10-04T06:00:00Z");
        var failureAt = Instant.parse("2026-10-04T07:00:00Z");
        statuses.recordSuccess("JSM", successAt);
        statuses.recordFailure("JSM", failureAt, "Authorization: Bearer configured-token-fixture");

        mockMvc.perform(get("/api/system/integrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrations[0].provider").value("JSM"))
                .andExpect(jsonPath("$.integrations[0].lastSuccessAt").value(successAt.toString()))
                .andExpect(jsonPath("$.integrations[0].lastFailureAt").value(failureAt.toString()))
                .andExpect(jsonPath("$.integrations[0].safeMessage").value("Integration request failed"))
                .andExpect(content().string(not(containsString("configured-token-fixture"))));
    }
}
