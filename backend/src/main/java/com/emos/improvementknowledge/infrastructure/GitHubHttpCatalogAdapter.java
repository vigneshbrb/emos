package com.emos.improvementknowledge.infrastructure;
import com.emos.improvementknowledge.application.*;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;

@Component @ConditionalOnProperty(name="emos.github.enabled",havingValue="true") public class GitHubHttpCatalogAdapter implements GitHubCatalogPort {
 private final RestClient http; private final JsonMapper json; private final String org;
 public GitHubHttpCatalogAdapter(RestClient.Builder builder, JsonMapper json,
  @Value("${emos.github.base-url:https://api.github.com}") String baseUrl,
  @Value("${emos.github.token:}") String token,@Value("${emos.github.organization:}") String org) {
  this.http=builder.baseUrl(baseUrl).defaultHeader("Authorization","Bearer "+token).build(); this.json=json; this.org=org;
 }
 public CatalogRefreshResult refresh() {
  try {
   var root=json.readTree(http.get().uri("/orgs/{org}/repos",org).retrieve().body(String.class)); var repos=new ArrayList<RepositoryDocument>();
   for(var node:root) { var name=node.get("name").asText(); var revision=node.get("default_branch").asText();
    repos.add(new RepositoryDocument(name, node.get("description")==null?"":node.get("description").asText(),
     read(node.get("topics")), permittedDocuments(name,revision), revision, Instant.now(), true)); }
   return new CatalogRefreshResult(List.copyOf(repos));
  } catch(Exception e) { throw new IllegalStateException("GitHub catalog refresh failed",e); }
 }
 private static Set<String> read(tools.jackson.databind.JsonNode node) { var out=new LinkedHashSet<String>(); if(node!=null) for(var n:node) out.add(n.asText()); return out; }
 private List<String> permittedDocuments(String repository,String revision){
  var out=new ArrayList<String>();
  for(var path:List.of("README.md","CODEOWNERS",".github/CODEOWNERS","deployment.yaml","deploy.yaml","k8s/deployment.yaml")){
   try { var body=http.get().uri("/repos/"+org+"/"+repository+"/contents/"+path+"?ref="+revision).retrieve().body(String.class); if(body!=null)out.add(path+": "+body); }
   catch(RestClientResponseException e){if(e.getStatusCode().value()!=404)throw e;}
  }
  return List.copyOf(out);
 }
}
