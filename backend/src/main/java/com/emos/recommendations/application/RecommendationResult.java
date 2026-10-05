package com.emos.recommendations.application;

import java.util.List;

public record RecommendationResult(
    String recommendedDisposition,
    String summary,
    String proposedImprovement,
    List<String> repositorySearchTerms,
    List<String> citations,
    String uncertainty,
    String model) {
  public RecommendationResult {
    repositorySearchTerms = List.copyOf(repositorySearchTerms);
    citations = List.copyOf(citations);
  }
}
