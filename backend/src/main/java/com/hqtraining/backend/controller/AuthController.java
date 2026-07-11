package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.dto.LoginRequest;
import com.hqtraining.backend.dto.LoginResponse;
import com.hqtraining.backend.dto.StudentRegisterRequest;
import com.hqtraining.backend.dto.UserInfoResponse;
import com.hqtraining.backend.model.MenuItem;
import com.hqtraining.backend.model.RegistrationReviewRecord;
import com.hqtraining.backend.service.AccountService;
import com.hqtraining.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;

    public AuthController(AuthService authService, AccountService accountService) {
        this.authService = authService;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.username(), request.password()));
    }

    @PostMapping("/register/student")
    public ApiResponse<RegistrationReviewRecord> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        return ApiResponse.success(accountService.registerStudent(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> currentUser(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return ApiResponse.success(authService.getCurrentUser(authorizationHeader));
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuItem>> menus(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return ApiResponse.success(authService.getMenus(authorizationHeader));
    }
}
