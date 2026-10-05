package com.emos.improvementknowledge.application;

import java.time.LocalDate;

public record ApprovedJiraDraft(
    String title,
    String problemStatement,
    String proposedDirection,
    String acceptanceIntent,
    String repositoryId,
    LocalDate reviewDate) {}
