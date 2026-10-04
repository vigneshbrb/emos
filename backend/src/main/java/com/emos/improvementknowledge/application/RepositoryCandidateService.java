package com.emos.improvementknowledge.application;
import com.emos.operationalobservation.domain.OperationalCaseId;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name="emos.persistence.enabled",havingValue="true",matchIfMissing=true)
public class RepositoryCandidateService {
 private final RepositoryCatalog catalog; private final RepositoryMappingLookup mappings; private final RepositorySignalSource signals;
 public RepositoryCandidateService(RepositoryCatalog catalog, RepositoryMappingLookup mappings, RepositorySignalSource signals) {
  this.catalog=catalog; this.mappings=mappings; this.signals=signals;
 }
 public List<RepositoryCandidate> candidatesFor(OperationalCaseId caseId) {
  var signal=signals.signalsFor(caseId); var documents=catalog.findFor(caseId); var result=new ArrayList<RepositoryCandidate>();
  mappings.findFor(caseId).ifPresent(mapping -> result.add(new RepositoryCandidate(mapping.repositoryId(), mapping.accessible(), mapping.accessible(), !mapping.accessible(),
    List.of(mapping.accessible()?"Confirmed mapping for monitor "+mapping.monitorId():"Confirmed repository is inaccessible; manager reconfirmation required"), "MANAGER_CONFIRMATION")));
  var mapped=result.stream().map(RepositoryCandidate::repositoryId).collect(java.util.stream.Collectors.toSet());
  documents.stream().filter(d -> !mapped.contains(d.repositoryId())).map(d -> candidate(d,signal)).filter(Objects::nonNull)
    .sorted(Comparator.comparing((RepositoryCandidate c)->c.provenance().equals("METADATA")?0:1).thenComparing(RepositoryCandidate::repositoryId)).forEach(result::add);
  return List.copyOf(result);
 }
 private static RepositoryCandidate candidate(RepositoryDocument d, CaseSignals s) {
  var hay=(d.repositoryId()+" "+d.description()+" "+d.topics()+" "+d.permittedDocuments()).toLowerCase();
  var matches=s.deterministicTerms().stream().filter(t->hay.contains(t.toLowerCase())).toList();
  if(!matches.isEmpty()) return new RepositoryCandidate(d.repositoryId(),false,d.accessible(),true,List.of("Metadata/document matches: "+matches),"METADATA");
  if(s.aiHints().contains(d.repositoryId())) return new RepositoryCandidate(d.repositoryId(),false,d.accessible(),true,List.of("AI search hint; requires manager confirmation"),"AI_HINT");
  return null;
 }
 public record CaseSignals(String monitorId,List<String> deterministicTerms,List<String> aiHints) {}
}
