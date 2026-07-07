package com.hqtraining.backend.dto;

public record LoginResponse(
        String token,
        UserInfoResponse userInfo
) {
}
