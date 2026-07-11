package com.hqtraining.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttendanceMaterialRequest(
        @NotBlank(message = "资料发放状态不能为空")
        @Size(max = 20, message = "资料发放状态长度不能超过20")
        String materialStatus,

        @Size(max = 255, message = "资料发放备注长度不能超过255")
        String materialRemark
) {
}
