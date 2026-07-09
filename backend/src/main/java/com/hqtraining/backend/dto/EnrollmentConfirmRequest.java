package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentConfirmRequest(
        @NotNull(message = "审核结果不能为空")
        Boolean approved,

        @Size(max = 255, message = "驳回原因长度不能超过255")
        String rejectReason
) {
}
