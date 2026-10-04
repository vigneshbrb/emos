package com.emos.improvementknowledge.domain;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.*;import java.util.UUID;
public record JiraDraft(UUID id,OperationalCaseId caseId,String title,String problemStatement,String evidenceSummary,String proposedDirection,String acceptanceIntent,String repositoryId,LocalDate reviewDate,boolean approved,Instant updatedAt){}
