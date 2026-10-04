package com.emos.improvementknowledge.application;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Clock;
import java.util.UUID;
@Service @ConditionalOnProperty(name="emos.persistence.enabled",havingValue="true",matchIfMissing=true) public class RepositoryMappingService {
 private final RepositoryMappingRepository repository; private final Clock clock;
 public RepositoryMappingService(RepositoryMappingRepository repository, Clock clock){this.repository=repository;this.clock=clock;}
 @Transactional public MappingId confirmMonitorMapping(OperationalCaseId caseId,String monitorId,RepositoryRef ref,String rationale){
  if(caseId==null||monitorId==null||monitorId.isBlank()||rationale==null||rationale.isBlank()) throw new IllegalArgumentException("Case, monitor, repository, and rationale are required");
  var id=new MappingId(UUID.randomUUID()); repository.confirm(id,caseId,monitorId,ref,rationale.strip(),clock.instant()); return id;
 }
 public java.util.Optional<RepositoryMapping> findCurrent(String monitorId){return repository.findCurrent(monitorId);}
}
