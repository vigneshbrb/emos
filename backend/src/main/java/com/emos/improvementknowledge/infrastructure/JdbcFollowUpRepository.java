package com.emos.improvementknowledge.infrastructure;
import com.emos.improvementknowledge.application.*;import com.emos.operationalobservation.domain.OperationalCaseId;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;import java.sql.*;import java.util.*;
@Repository @ConditionalOnProperty(name="emos.persistence.enabled",havingValue="true",matchIfMissing=true)
public class JdbcFollowUpRepository implements FollowUpRepository{
 private final JdbcTemplate jdbc;public JdbcFollowUpRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<FollowUpRecord> find(FollowUpId id){return jdbc.query("select * from improvement_follow_up where id=?",this::map,id.value()).stream().findFirst();}
 public Optional<FollowUpRecord> findForCase(OperationalCaseId id){return jdbc.query("select * from improvement_follow_up where case_id=? order by created_at desc limit 1",this::map,id.value()).stream().findFirst();}
 public List<FollowUpRecord> findOpen(){return jdbc.query("select * from improvement_follow_up where state='OPEN' order by review_date,id",this::map);}
 public void save(FollowUpRecord r){jdbc.update("update improvement_follow_up set state=?,jira_status=?,observed_at=?,source_accessible=?,final_rationale_required=? where id=?",r.state(),r.jiraStatus(),Timestamp.from(r.observedAt()),r.accessible(),r.finalRationaleRequired(),r.id().value());}
 private FollowUpRecord map(ResultSet rs,int n)throws SQLException{return new FollowUpRecord(new FollowUpId(rs.getObject("id",UUID.class)),new OperationalCaseId(rs.getObject("case_id",UUID.class)),new JiraIssueRef(rs.getString("jira_key"),rs.getString("jira_url")),rs.getObject("review_date",java.time.LocalDate.class),rs.getString("state"),rs.getString("jira_status"),rs.getTimestamp("observed_at")==null?null:rs.getTimestamp("observed_at").toInstant(),rs.getBoolean("source_accessible"),rs.getBoolean("final_rationale_required"));}
}
