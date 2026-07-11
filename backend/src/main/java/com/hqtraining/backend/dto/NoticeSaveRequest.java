package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record NoticeSaveRequest(
        @NotNull(message = "课程不能为空")
        Long courseId,

        @NotBlank(message = "通知标题不能为空")
        @Size(max = 200, message = "通知标题长度不能超过200")
        String title,

        @NotBlank(message = "通知内容不能为空")
        @Size(max = 4000, message = "通知内容长度不能超过4000")
        String content,

        LocalDateTime registrationStartAt,

        LocalDateTime registrationEndAt,

        Boolean externalPublishFlag
) {
}
