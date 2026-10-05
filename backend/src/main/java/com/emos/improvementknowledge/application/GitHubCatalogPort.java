package com.emos.improvementknowledge.application;

import java.util.List;

public interface GitHubCatalogPort {
  CatalogRefreshResult refresh();

  record CatalogRefreshResult(List<RepositoryDocument> repositories) {}
}
