package com.emos.improvementknowledge.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "emos.github.enabled", havingValue = "true")
public class RepositoryCatalogRefreshService {
  private final GitHubCatalogPort github;
  private final RepositoryCatalogStore catalog;

  public RepositoryCatalogRefreshService(GitHubCatalogPort github, RepositoryCatalogStore catalog) {
    this.github = github;
    this.catalog = catalog;
  }

  @Transactional
  public GitHubCatalogPort.CatalogRefreshResult refresh() {
    var result = github.refresh();
    catalog.replaceAll(result.repositories());
    return result;
  }
}
