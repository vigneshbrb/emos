package com.emos.improvementknowledge.application;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.Optional;
@FunctionalInterface public interface RepositoryMappingLookup { Optional<RepositoryMapping> findFor(OperationalCaseId caseId); }
