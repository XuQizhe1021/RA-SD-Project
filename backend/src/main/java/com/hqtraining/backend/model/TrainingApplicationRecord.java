package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingApplicationRecord(
        Long id,
        String applicationNo,
        Long companyId,
        String companyName,
        Long applicantUserId,
        String applicantName,
        String topic,
        LocalDate expectedStartDate,
        LocalDate expectedEndDate,
        Integer attendeeCount,
        BigDecimal budgetAmount,
        String requirementDesc,
        String status,
        String approvalComment,
        Long approvedBy,
        String approvedByName,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
