package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationOptionRecord(
        Long id,
        String applicationNo,
        String companyName,
        String topic,
        LocalDate expectedStartDate,
        LocalDate expectedEndDate,
        Integer attendeeCount,
        BigDecimal budgetAmount
) {
}
