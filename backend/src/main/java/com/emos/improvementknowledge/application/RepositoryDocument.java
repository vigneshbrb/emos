package com.emos.improvementknowledge.application;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record RepositoryDocument(
    String repositoryId,
    String description,
    Set<String> topics,
    List<String> permittedDocuments,
    String revision,
    Instant retrievedAt,
    boolean accessible) {}
