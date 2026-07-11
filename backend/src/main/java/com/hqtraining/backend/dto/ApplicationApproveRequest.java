package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApplicationApproveRequest(
        @NotNull(message = "审批结果不能为空")
        Boolean approved,

        @Size(max = 255, message = "审批意见长度不能超过255")
        String approvalComment
) {
}
