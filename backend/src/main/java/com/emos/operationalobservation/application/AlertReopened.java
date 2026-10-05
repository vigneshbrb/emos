package com.emos.operationalobservation.application;

import com.emos.operationalobservation.domain.OperationalCaseId;
import java.time.Instant;

public record AlertReopened(OperationalCaseId caseId, Instant reopenedAt) {}
