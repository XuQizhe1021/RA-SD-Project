package com.hqtraining.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EvaluationSubmitRequest(
        @NotNull(message = "课程ID不能为空")
        Long courseId,

        Long studentId,

        @NotNull(message = "报名记录ID不能为空")
        Long enrollmentId,

        @NotNull(message = "满意度评分不能为空")
        @Min(value = 1, message = "满意度评分不能低于1分")
        @Max(value = 5, message = "满意度评分不能高于5分")
        Integer rating,

        @Size(max = 500, message = "评价意见不能超过500字")
        String commentText
) {
}
