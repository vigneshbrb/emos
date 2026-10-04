package com.emos.improvementknowledge.application;
import com.emos.operationalobservation.domain.OperationalCaseId;import java.time.LocalDate;import java.util.UUID;
public record CreateImprovementCommand(OperationalCaseId caseId,UUID draftId,UUID attemptId,LocalDate reviewDate){}
