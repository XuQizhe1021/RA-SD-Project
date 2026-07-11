package com.hqtraining.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationSaveRequest(
        @NotBlank(message = "企业名称不能为空")
        @Size(max = 150, message = "企业名称长度不能超过150")
        String companyName,

        @NotBlank(message = "培训主题不能为空")
        @Size(max = 200, message = "培训主题长度不能超过200")
        String topic,

        LocalDate expectedStartDate,

        LocalDate expectedEndDate,

        @NotNull(message = "预计参训人数不能为空")
        @Min(value = 1, message = "预计参训人数至少为1")
        Integer attendeeCount,

        @DecimalMin(value = "0.00", message = "预算金额不能小于0")
        BigDecimal budgetAmount,

        @Size(max = 4000, message = "需求描述长度不能超过4000")
        String requirementDesc
) {
}
