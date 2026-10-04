package com.emos.recommendations.application;

public interface RecommendationProvider {
    RecommendationResult generate(RecommendationRequest request);
}
