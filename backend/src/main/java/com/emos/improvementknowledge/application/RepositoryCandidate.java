package com.emos.improvementknowledge.application;

import java.util.List;

public record RepositoryCandidate(
    String repositoryId,
    boolean authoritative,
    boolean accessible,
    boolean requiresConfirmation,
    List<String> reasons,
    String provenance) {}
