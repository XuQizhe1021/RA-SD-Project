package com.hqtraining.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseSaveRequest(
        Long applicationId,

        @NotBlank(message = "课程名称不能为空")
        @Size(max = 200, message = "课程名称长度不能超过200")
        String courseName,

        Long lecturerId,

        @NotNull(message = "开始时间不能为空")
        LocalDateTime startTime,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,

        @NotBlank(message = "培训地点不能为空")
        @Size(max = 200, message = "培训地点长度不能超过200")
        String location,

        @NotNull(message = "名额不能为空")
        @Min(value = 1, message = "名额至少为1")
        Integer quota,

        @NotNull(message = "培训费用不能为空")
        @DecimalMin(value = "0.00", message = "培训费用不能小于0")
        BigDecimal feeAmount
) {
}
