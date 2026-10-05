package com.emos.recommendations.application;

import java.util.List;

public record RecommendationRequest(String caseId, List<Evidence> evidence) {
  public RecommendationRequest {
    evidence = List.copyOf(evidence);
  }

  public record Evidence(String id, String content) {}
}
