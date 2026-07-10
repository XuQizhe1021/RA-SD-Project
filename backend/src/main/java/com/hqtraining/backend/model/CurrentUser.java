package com.hqtraining.backend.model;

import java.util.List;

public record CurrentUser(
        Long id,
        String username,
        String displayName,
        String accountType,
        List<String> roles
) {
    public boolean hasRole(String roleCode) {
        return roles != null && roles.contains(roleCode);
    }
}
