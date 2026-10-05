package com.emos.attentionfollowthrough.infrastructure;

import com.emos.attentionfollowthrough.application.AttentionItemRepository;
import com.emos.attentionfollowthrough.domain.AttentionItem;
import java.sql.Timestamp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(
    name = "emos.persistence.enabled",
    havingValue = "true",
    matchIfMissing = true)
class JdbcAttentionItemRepository implements AttentionItemRepository {
  private final JdbcTemplate jdbc;

  JdbcAttentionItemRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insertIfAbsent(AttentionItem item) {
    jdbc.update(
        """
                insert into attention_item (id, obligation_id, subject_case_id, reason, state, opened_at)
                values (?, ?, ?, ?, ?, ?) on conflict (obligation_id) do nothing
                """,
        item.id(),
        item.obligationId().value(),
        item.caseId().value(),
        item.reason(),
        item.state().name(),
        Timestamp.from(item.openedAt()));
  }
}
