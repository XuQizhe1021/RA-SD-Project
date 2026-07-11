package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.dto.EvaluationSubmitRequest;
import com.hqtraining.backend.model.CourseEvaluationReport;
import com.hqtraining.backend.model.CourseEvaluationSummary;
import com.hqtraining.backend.model.CourseOptionRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.EvaluationCandidateRecord;
import com.hqtraining.backend.model.EvaluationRecordView;
import com.hqtraining.backend.model.PendingEvaluationCourse;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final AuthService authService;

    public EvaluationController(EvaluationService evaluationService, AuthService authService) {
        this.evaluationService = evaluationService;
        this.authService = authService;
    }

    @GetMapping("/pending-courses")
    public ApiResponse<List<PendingEvaluationCourse>> pendingCourses(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(evaluationService.getPendingCourses(currentUser));
    }

    @GetMapping("/mine")
    public ApiResponse<List<EvaluationRecordView>> myEvaluations(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(evaluationService.getMyEvaluations(currentUser));
    }

    @GetMapping("/proxy-courses")
    public ApiResponse<List<CourseOptionRecord>> proxyCourses(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(evaluationService.getProxyCourseOptions(currentUser));
    }

    @GetMapping("/proxy-candidates")
    public ApiResponse<List<EvaluationCandidateRecord>> proxyCandidates(
            @RequestParam Long courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(evaluationService.getProxyCandidates(courseId, currentUser));
    }

    @GetMapping("/summaries")
    public ApiResponse<List<CourseEvaluationSummary>> summaries(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Boolean hasEvaluation,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(
                evaluationService.getCourseEvaluationSummaries(keyword, startDate, endDate, hasEvaluation, currentUser)
        );
    }

    @GetMapping("/courses/{courseId}/report")
    public ApiResponse<CourseEvaluationReport> report(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(evaluationService.getCourseEvaluationReport(courseId, currentUser));
    }

    @PostMapping
    public ApiResponse<EvaluationRecordView> submit(
            @Valid @RequestBody EvaluationSubmitRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(evaluationService.submitEvaluation(request, currentUser));
    }
}
