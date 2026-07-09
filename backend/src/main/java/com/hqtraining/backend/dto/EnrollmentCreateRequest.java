package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentCreateRequest(
        @NotNull(message = "课程不能为空")
        Long courseId,

        @NotNull(message = "学员不能为空")
        Long studentId,

        @NotBlank(message = "付费类型不能为空")
        @Size(max = 30, message = "付费类型长度不能超过30")
        String paymentType
) {
}
