package com.hqtraining.backend.dto;

import jakarta.validation.constraints.Size;

public record AttendanceCheckInRequest(
        @Size(max = 255, message = "签到备注长度不能超过255")
        String remark
) {
}
