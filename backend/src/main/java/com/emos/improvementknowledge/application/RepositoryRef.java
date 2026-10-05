package com.emos.improvementknowledge.application;

public record RepositoryRef(String value) {
  public RepositoryRef {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Repository is required");
  }
}
