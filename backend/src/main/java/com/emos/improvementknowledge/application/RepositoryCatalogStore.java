package com.emos.improvementknowledge.application;
import java.util.List;
public interface RepositoryCatalogStore extends RepositoryCatalog { void replaceAll(List<RepositoryDocument> documents); }
