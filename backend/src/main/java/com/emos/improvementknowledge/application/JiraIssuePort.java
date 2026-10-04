package com.emos.improvementknowledge.application;
import java.util.Optional;
public interface JiraIssuePort { JiraIssueRef create(ApprovedJiraDraft draft,String correlationKey); Optional<JiraIssueRef> findByCorrelationKey(String correlationKey); default JiraIssueObservation observe(JiraIssueRef issue){throw new UnsupportedOperationException();} }
