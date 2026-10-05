package com.emos.web;

import com.emos.platform.diagnostics.IntegrationStatus;
import com.emos.platform.diagnostics.IntegrationStatusRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SystemDiagnosticsController {
  private final IntegrationStatusRepository statuses;

  public SystemDiagnosticsController(IntegrationStatusRepository statuses) {
    this.statuses = statuses;
  }

  @GetMapping("/integrations")
  public IntegrationStatuses integrations() {
    return new IntegrationStatuses(statuses.findAll());
  }

  public record IntegrationStatuses(List<IntegrationStatus> integrations) {}
}
