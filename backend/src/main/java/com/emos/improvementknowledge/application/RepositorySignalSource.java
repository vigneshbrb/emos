package com.emos.improvementknowledge.application;
import com.emos.operationalobservation.domain.OperationalCaseId;
@FunctionalInterface public interface RepositorySignalSource { RepositoryCandidateService.CaseSignals signalsFor(OperationalCaseId caseId); }
