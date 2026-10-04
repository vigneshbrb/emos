package com.emos.improvementknowledge.domain;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.*;import java.util.UUID;
public record ImprovementFollowUp(UUID id,OperationalCaseId caseId,String jiraKey,String jiraUrl,LocalDate reviewDate,String state,String finalRationale,Instant createdAt){}
