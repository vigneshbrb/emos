package com.emos.improvementknowledge.infrastructure;

import com.emos.improvementknowledge.application.*;
import com.emos.platform.jobs.*;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "emos.jira.enabled", havingValue = "true")
public class JiraPollingJobHandler implements JobHandler {
  private final JiraFollowUpService service;

  public JiraPollingJobHandler(JiraFollowUpService service) {
    this.service = service;
  }

  public String type() {
    return "jira.followup.refresh";
  }

  public void handle(Job job) {
    service.refresh(
        new FollowUpId(UUID.fromString(job.payload().path("followUpId").stringValue())));
  }
}
