package com.emos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:17:///emos",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
    })
class PostgresIntegrationTest {

  @Test
  void contextLoadsAgainstPostgres17() {}
}
