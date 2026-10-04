package com.emos.platform.audit;

public interface AuditTrail {
    AuditEntry append(AuditEntry entry);
}
