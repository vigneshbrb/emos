package com.emos.attentionfollowthrough.domain;

public record ExpectationId(String value) {
    public ExpectationId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Expectation ID is required");
    }
}
