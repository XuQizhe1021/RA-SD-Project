package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.NoticeSaveRequest;
import com.hqtraining.backend.model.CourseNoticeRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final AuthService authService;

    public NoticeController(NoticeService noticeService, AuthService authService) {
        this.noticeService = noticeService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<CourseNoticeRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(noticeService.getNotices(pageNum, pageSize, keyword, status, courseId, currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseNoticeRecord> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(noticeService.getNoticeById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<CourseNoticeRecord> create(
            @Valid @RequestBody NoticeSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(noticeService.createNotice(request, currentUser));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseNoticeRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody NoticeSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(noticeService.updateNotice(id, request, currentUser));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CourseNoticeRecord> publish(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(noticeService.publishNotice(id, currentUser));
    }

    @PostMapping("/{id}/revoke")
    public ApiResponse<CourseNoticeRecord> revoke(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(noticeService.revokeNotice(id, currentUser));
    }
}
