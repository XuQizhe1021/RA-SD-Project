package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.ApplicationApproveRequest;
import com.hqtraining.backend.dto.ApplicationSaveRequest;
import com.hqtraining.backend.model.ApplicationOptionRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.TrainingApplicationRecord;
import com.hqtraining.backend.service.ApplicationService;
import com.hqtraining.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AuthService authService;

    public ApplicationController(ApplicationService applicationService, AuthService authService) {
        this.applicationService = applicationService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<TrainingApplicationRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(applicationService.getApplications(pageNum, pageSize, keyword, status, currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<TrainingApplicationRecord> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(applicationService.getApplicationById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<TrainingApplicationRecord> create(
            @Valid @RequestBody ApplicationSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(applicationService.createApplication(request, currentUser));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<TrainingApplicationRecord> approve(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationApproveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(applicationService.approveApplication(id, request, currentUser));
    }

    @GetMapping("/options/approved")
    public ApiResponse<List<ApplicationOptionRecord>> approvedOptions(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(applicationService.getApprovedApplicationOptions(currentUser));
    }
}
