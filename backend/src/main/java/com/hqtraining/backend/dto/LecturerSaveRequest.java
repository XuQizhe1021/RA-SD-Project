package com.hqtraining.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LecturerSaveRequest(
        @NotBlank(message = "讲师姓名不能为空")
        @Size(max = 100, message = "讲师姓名长度不能超过100")
        String fullName,

        @Size(max = 100, message = "职称长度不能超过100")
        String title,

        @Size(max = 200, message = "专长方向长度不能超过200")
        String specialty,

        @Size(max = 30, message = "联系电话长度不能超过30")
        String phone,

        @Size(max = 100, message = "邮箱长度不能超过100")
        String email,

        @DecimalMin(value = "0.00", message = "费用标准不能小于0")
        BigDecimal feeStandard,

        String profileText
) {
}
