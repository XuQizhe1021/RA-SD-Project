package com.hqtraining.backend.model;

import java.util.List;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String email,
        List<String> roles
) {
}
