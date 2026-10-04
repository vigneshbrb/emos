package com.emos.attentionfollowthrough.domain;

import java.util.UUID;

public record ObligationId(UUID value) {
    public ObligationId { if (value == null) throw new IllegalArgumentException("Obligation ID is required"); }
    public static ObligationId newId() { return new ObligationId(UUID.randomUUID()); }
}
