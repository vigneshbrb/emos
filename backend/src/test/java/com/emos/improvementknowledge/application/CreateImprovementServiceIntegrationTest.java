package com.emos.improvementknowledge.application;
import com.emos.operationalobservation.domain.OperationalCaseId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(CreateImprovementServiceIntegrationTest.Config.class)
@TestPropertySource(properties={"spring.datasource.url=jdbc:tc:postgresql:17:///emos-create-improvement","spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"})
class CreateImprovementServiceIntegrationTest {
 @Autowired CreateImprovementService service; @Autowired JiraDraftService draftService; @Autowired JdbcTemplate jdbc; @Autowired StubJira jira;
 OperationalCaseId caseId; UUID draftId; UUID obligationId;
 @BeforeEach void seed(){jdbc.execute("truncate table improvement_follow_up, disposition_attempt, jira_draft, repository_mapping_current, repository_mapping_history, attention_item, disposition_obligation, operational_evidence_snapshot, operational_alert cascade");jira.issue=Optional.empty();jira.creates=0;
  caseId=new OperationalCaseId(UUID.randomUUID());draftId=UUID.randomUUID();obligationId=UUID.randomUUID();var now=Instant.now();
  jdbc.update("insert into jira_draft(id,case_id,title,problem_statement,evidence_summary,proposed_direction,acceptance_intent,repository_id,review_date,approved,updated_at) values(?,?,?,?,?,?,?,?,?,?,?)",draftId,caseId.value(),"Latency","Recurring latency","3 occurrences","Profile SQL","Stable latency","payments",LocalDate.now().plusDays(7),true,java.sql.Timestamp.from(now));
  jdbc.update("insert into disposition_obligation(id,expectation_key,policy_version,subject_case_id,source_resolved_at,deadline,state) values(?,?,?,?,?,?,?)",obligationId,"operational-case-disposition",1,caseId.value(),java.sql.Timestamp.from(now.minusSeconds(60)),java.sql.Timestamp.from(now.plusSeconds(3600)),"PENDING");
  var history=UUID.randomUUID();jdbc.update("insert into repository_mapping_history(id,case_id,monitor_id,repository_id,rationale,confirmed_at) values(?,?,?,?,?,?)",history,caseId.value(),"monitor-1","payments","confirmed",java.sql.Timestamp.from(now));jdbc.update("insert into repository_mapping_current(monitor_id,history_id,repository_id,rationale,confirmed_at) values(?,?,?,?,?)","monitor-1",history,"payments","confirmed",java.sql.Timestamp.from(now));}
 @Test void creates_once_then_atomically_satisfies_disposition_and_opens_follow_up(){var attempt=UUID.randomUUID();var review=LocalDate.now().plusDays(7);
  var result=service.execute(new CreateImprovementCommand(caseId,draftId,attempt,review));
  assertThat(result.issue().key()).isEqualTo("OPS-42");assertThat(jira.creates).isEqualTo(1);
  assertThat(jdbc.queryForObject("select state from disposition_obligation where id=?",String.class,obligationId)).isEqualTo("SATISFIED");
  assertThat(jdbc.queryForObject("select count(*) from improvement_follow_up",Integer.class)).isEqualTo(1);
  assertThat(service.execute(new CreateImprovementCommand(caseId,draftId,attempt,review)).issue().key()).isEqualTo("OPS-42");assertThat(jira.creates).isEqualTo(1);
 }
 @Test void rejects_unapproved_or_past_review_date(){jdbc.update("update jira_draft set approved=false where id=?",draftId);
  assertThatThrownBy(()->service.execute(new CreateImprovementCommand(caseId,draftId,UUID.randomUUID(),LocalDate.now().plusDays(7)))).isInstanceOf(ImprovementValidationException.class);
  jdbc.update("update jira_draft set approved=true where id=?",draftId);
  assertThatThrownBy(()->service.execute(new CreateImprovementCommand(caseId,draftId,UUID.randomUUID(),LocalDate.now().minusDays(1)))).isInstanceOf(ImprovementValidationException.class);
 }
 @Test void rejects_missing_review_unconfirmed_repository_and_satisfied_obligation(){
  assertThatThrownBy(()->service.execute(new CreateImprovementCommand(caseId,draftId,UUID.randomUUID(),null))).isInstanceOf(ImprovementValidationException.class);
  jdbc.execute("delete from repository_mapping_current");
  assertThatThrownBy(()->service.execute(new CreateImprovementCommand(caseId,draftId,UUID.randomUUID(),LocalDate.now().plusDays(7)))).isInstanceOf(ImprovementValidationException.class);
  jdbc.update("update disposition_obligation set state='SATISFIED' where id=?",obligationId);
  jdbc.update("insert into repository_mapping_current(monitor_id,history_id,repository_id,rationale,confirmed_at) select monitor_id,id,repository_id,rationale,confirmed_at from repository_mapping_history limit 1");
  assertThatThrownBy(()->service.execute(new CreateImprovementCommand(caseId,draftId,UUID.randomUUID(),LocalDate.now().plusDays(7)))).isInstanceOf(ImprovementValidationException.class);
 }
 @Test void prepared_draft_suggests_editable_review_date_seven_calendar_days_from_linking(){
  jdbc.update("delete from jira_draft where case_id=?",caseId.value());var now=Instant.now();
  jdbc.update("insert into operational_alert(case_id,source_id,status,severity,source_updated_at,domain_version) values(?,?,'RESOLVED','P2',?,1)",caseId.value(),"alert-"+caseId.value(),java.sql.Timestamp.from(now));
  jdbc.update("insert into operational_evidence_snapshot(id,snapshot_key,case_id,observed_at,monitor_id,jsm_severity,latest_duration_seconds,recurrence_24h,recurrence_7d,recurrence_30d,provider_occurrence_ids,source_links) values(?,?,?,?,?,?,?,?,?,?,?::jsonb,?::jsonb)",UUID.randomUUID(),"evidence-"+caseId.value(),caseId.value(),java.sql.Timestamp.from(now),"monitor-1","P2",900L,3,7,12,"[]","[]");
  var draft=draftService.prepare(caseId);assertThat(draft.reviewDate()).isEqualTo(LocalDate.now().plusDays(7));
  assertThat(draftService.update(new com.emos.improvementknowledge.domain.JiraDraft(draft.id(),draft.caseId(),draft.title(),draft.problemStatement(),draft.evidenceSummary(),draft.proposedDirection(),draft.acceptanceIntent(),draft.repositoryId(),draft.reviewDate().plusDays(2),false,draft.updatedAt())).reviewDate()).isEqualTo(draft.reviewDate().plusDays(2));
 }
 @TestConfiguration static class Config{@Bean @Primary StubJira jiraIssuePort(){return new StubJira();}}
 static class StubJira implements JiraIssuePort{int creates;Optional<JiraIssueRef> issue=Optional.empty();public Optional<JiraIssueRef> findByCorrelationKey(String key){return issue;}public JiraIssueRef create(ApprovedJiraDraft d,String key){creates++;var ref=new JiraIssueRef("OPS-42","https://jira/OPS-42");issue=Optional.of(ref);return ref;}}
}
