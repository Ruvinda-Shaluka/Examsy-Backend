package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.ExamPublishDTO;
import lk.ijse.examsybackend.dto.ExamSummaryDTO;
import lk.ijse.examsybackend.service.TeacherExamService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/exams")
@RequiredArgsConstructor
public class TeacherExamController {

    private final TeacherExamService teacherExamService;

    /**
     * Publishes a new exam (MCQ, Short Answer, or PDF) to one or multiple classes.
     */
    @PostMapping("/publish")
    public ResponseEntity<APIResponse<Void>> publishExam(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody ExamPublishDTO dto) {

        // The user.getUsername() ensures that a teacher can only publish exams for THEIR own classes
        teacherExamService.publishExam(user.getUsername(), dto);

        // 201 Created is the standard HTTP status code for successfully creating new data
        return ResponseEntity.status(201).body(new APIResponse<>(201, "Exam published successfully to selected classes.", null));
    }

    @GetMapping("/class/{classId}")
    public ResponseEntity<APIResponse<List<ExamSummaryDTO>>> getClassExams(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails user) {

        List<ExamSummaryDTO> exams = teacherExamService.getClassExams(user.getUsername(), classId);
        return ResponseEntity.ok(new APIResponse<>(200, "Exams retrieved successfully", exams));
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<APIResponse<Void>> deleteExam(
            @PathVariable Integer examId,
            @AuthenticationPrincipal UserDetails user) {

        teacherExamService.deleteExam(user.getUsername(), examId);
        return ResponseEntity.ok(new APIResponse<>(200, "Exam deleted successfully", null));
    }
}