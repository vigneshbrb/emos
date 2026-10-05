package com.emos.recommendations.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.Optional;
import java.util.UUID;

public interface RecommendationRepository {
  Optional<RecommendationRecord> findByKey(String key);

  Optional<RecommendationRecord> findById(UUID id);

  Optional<RecommendationRecord> findLatest(OperationalCaseId caseId);

  RecommendationRecord insert(RecommendationRecord record);

  void replace(RecommendationRecord record);
}
