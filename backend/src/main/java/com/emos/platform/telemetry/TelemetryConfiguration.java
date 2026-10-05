package com.emos.platform.telemetry;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class TelemetryConfiguration {
  @Bean
  InteractionSessionStore interactionSessionStore(JdbcTemplate jdbc) {
    return new JdbcInteractionSessionStore(jdbc);
  }

  @Bean
  InteractionSessionService interactionSessionService(
      InteractionSessionStore store,
      @Value("${emos.telemetry.idle-cutoff:PT120S}") Duration idleCutoff) {
    return new InteractionSessionService(store, idleCutoff);
  }
}
