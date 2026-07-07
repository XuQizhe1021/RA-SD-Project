package com.hqtraining.backend.dto;

import java.util.List;

public record UserInfoResponse(
        Long id,
        String username,
        String displayName,
        List<String> roles
) {
}
