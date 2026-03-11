package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.service.StudentExamService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/vault")
    public ResponseEntity<APIResponse<VaultExamsResponseDTO>> getStudentVault(
            @AuthenticationPrincipal UserDetails user) {

        VaultExamsResponseDTO vaultData = studentExamService.getVaultExams(user.getUsername());

        return ResponseEntity.ok(new APIResponse<>(200, "Vault loaded successfully", vaultData));
    }
}