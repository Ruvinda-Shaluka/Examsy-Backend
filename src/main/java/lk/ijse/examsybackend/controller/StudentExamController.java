package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.dto.request.ExamSubmitDTO;
import lk.ijse.examsybackend.dto.request.ProctoringDTO;
import lk.ijse.examsybackend.dto.request.ProctoringLogDTO;
import lk.ijse.examsybackend.dto.response.ExamResultDTO;
import lk.ijse.examsybackend.dto.response.StudentAnalyticsDTO;
import lk.ijse.examsybackend.dto.response.StudentExamViewDTO;
import lk.ijse.examsybackend.dto.response.VaultExamsResponseDTO;
import lk.ijse.examsybackend.service.StudentExamService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/exams")
@RequiredArgsConstructor
@Validated
public class StudentExamController {

    private final StudentExamService studentExamService;

    // This endpoint will be hit silently by the frontend hook
    @PostMapping("/proctoring/log")
    public ResponseEntity<APIResponse<Void>> logViolation(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ProctoringDTO dto) {

        studentExamService.logSecurityViolation(user.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Violation logged", null));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<APIResponse<StudentExamViewDTO>> getExam(@PathVariable Integer examId, @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", studentExamService.getExamForStudent(user.getUsername(), examId)));
    }

    @PostMapping("/{examId}/submit")
    public ResponseEntity<APIResponse<ExamResultDTO>> submitExam(
            @PathVariable Integer examId,
            @Valid @RequestBody ExamSubmitDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        ExamResultDTO result = studentExamService.submitExam(user.getUsername(), examId, dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Submitted", result));
    }

    @GetMapping("/vault/{classId}")
    public ResponseEntity<APIResponse<VaultExamsResponseDTO>> getStudentVault(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails user) {

        VaultExamsResponseDTO vaultData = studentExamService.getVaultExams(user.getUsername(), classId);

        return ResponseEntity.ok(new APIResponse<>(200, "Vault loaded successfully", vaultData));
    }

    @PostMapping("/{examId}/log-event")
    public ResponseEntity<APIResponse<ProctoringStatsDTO>> logProctoringEvent(
            @PathVariable Integer examId,
            @Valid @RequestBody ProctoringLogDTO logDTO,
            @AuthenticationPrincipal UserDetails user) {

        ProctoringStatsDTO updatedStats = studentExamService.logProctoringEvent(examId, user.getUsername(), logDTO);
        return ResponseEntity.ok(new APIResponse<>(200, "Proctoring event logged", updatedStats));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<APIResponse<StudentAnalyticsDTO>> getStudentAnalytics(@AuthenticationPrincipal UserDetails user) {
        StudentAnalyticsDTO analytics = studentExamService.getStudentAnalytics(user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Analytics fetched successfully", analytics));
    }
}