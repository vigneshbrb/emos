package com.emos.attentionfollowthrough.application;
import com.emos.operationalobservation.domain.OperationalCaseId;import java.time.Instant;
public interface DispositionCompletionPort{boolean hasActive(OperationalCaseId caseId);boolean complete(OperationalCaseId caseId,Instant at);}
