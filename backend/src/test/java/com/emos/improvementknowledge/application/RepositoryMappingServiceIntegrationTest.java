package com.emos.improvementknowledge.application;

import static org.assertj.core.api.Assertions.*;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos-repository-mapping",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class RepositoryMappingServiceIntegrationTest {
  @Autowired RepositoryMappingService service;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute("truncate table repository_mapping_current, repository_mapping_history");
  }

  @Test
  void confirmation_requires_context_and_preserves_immutable_history_with_one_current_mapping() {
    var caseId = new OperationalCaseId(UUID.randomUUID());
    service.confirmMonitorMapping(
        caseId,
        "monitor-17",
        new RepositoryRef("payments"),
        "Runbook and deployment metadata match");
    service.confirmMonitorMapping(
        caseId, "monitor-17", new RepositoryRef("payments-v2"), "Service moved");
    assertThat(
            jdbc.queryForObject("select count(*) from repository_mapping_history", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "select repository_id from repository_mapping_current where monitor_id='monitor-17'",
                String.class))
        .isEqualTo("payments-v2");
    assertThatThrownBy(
            () ->
                service.confirmMonitorMapping(
                    caseId, "monitor-17", new RepositoryRef("payments"), " "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
