package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.dto.InternalAccountCreateRequest;
import com.hqtraining.backend.dto.RegistrationReviewRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.ManagedAccountRecord;
import com.hqtraining.backend.model.RegistrationReviewRecord;
import com.hqtraining.backend.service.AccountService;
import com.hqtraining.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;

    public AccountController(AccountService accountService, AuthService authService) {
        this.accountService = accountService;
        this.authService = authService;
    }

    @GetMapping("/registrations/pending")
    public ApiResponse<List<RegistrationReviewRecord>> pendingRegistrations(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(accountService.getPendingRegistrations(currentUser));
    }

    @PostMapping("/registrations/{userId}/review")
    public ApiResponse<RegistrationReviewRecord> reviewRegistration(
            @PathVariable Long userId,
            @Valid @RequestBody RegistrationReviewRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(accountService.reviewRegistration(userId, request, currentUser));
    }

    @GetMapping
    public ApiResponse<List<ManagedAccountRecord>> listAccounts(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(accountService.getManagedAccounts(currentUser));
    }

    @PostMapping("/internal")
    public ApiResponse<ManagedAccountRecord> createInternalAccount(
            @Valid @RequestBody InternalAccountCreateRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(accountService.createInternalAccount(request, currentUser));
    }
}
