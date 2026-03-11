package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.ProctoringDTO;
import lk.ijse.examsybackend.service.StudentExamService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/exams")
@RequiredArgsConstructor
public class StudentExamController {

    private final StudentExamService studentExamService;

    // This endpoint will be hit silently by the frontend hook
    @PostMapping("/proctoring/log")
    public ResponseEntity<APIResponse<Void>> logViolation(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody ProctoringDTO dto) {

        studentExamService.logSecurityViolation(user.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Violation logged", null));
    }
}