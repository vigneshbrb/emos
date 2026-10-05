package com.emos.improvementknowledge.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.List;

@FunctionalInterface
public interface RepositoryCatalog {
  List<RepositoryDocument> findFor(OperationalCaseId caseId);
}
