package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.util.List;

public record RevenueStatisticsResponse(
        BigDecimal receivableAmountTotal,
        BigDecimal paidAmountTotal,
        Integer specialPaymentCount,
        BigDecimal cashAmountRatio,
        BigDecimal transferAmountRatio,
        List<RevenueDetailRecord> details
) {
}
