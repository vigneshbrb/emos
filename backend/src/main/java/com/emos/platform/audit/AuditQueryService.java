package com.emos.platform.audit;

import java.util.List;
import java.util.UUID;

public interface AuditQueryService {
  List<AuditEntry> findForSubject(String subjectType, UUID subjectId);
}
