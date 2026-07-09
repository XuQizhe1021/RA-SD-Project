package com.hqtraining.backend.model;

public record StudentOptionRecord(
        Long id,
        String studentNo,
        String fullName,
        String companyName,
        String phone,
        String email
) {
}
