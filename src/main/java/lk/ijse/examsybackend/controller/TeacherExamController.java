package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.service.SmartGradingService;
import lk.ijse.examsybackend.service.TeacherExamService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/exams")
@RequiredArgsConstructor
public class TeacherExamController {

    private final TeacherExamService teacherExamService;
    private final SmartGradingService smartGradingService;

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

    @PutMapping("/{examId}/timing")
    public ResponseEntity<APIResponse<Void>> updateExamTiming(
            @PathVariable Integer examId,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody UpdateExamDeadlineDTO dto) {

        teacherExamService.updateExamTiming(user.getUsername(), examId, dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Exam timings updated successfully", null));
    }


    @GetMapping("/ongoing")
    public ResponseEntity<APIResponse<OngoingExamGroupDTO>> getOngoingExams(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", teacherExamService.getOngoingExams(user.getUsername())));
    }

    @GetMapping("/{examId}/monitor")
    public ResponseEntity<APIResponse<List<LiveStudentMonitorDTO>>> getLiveMonitorData(
            @PathVariable Integer examId,
            @AuthenticationPrincipal UserDetails user) {

        List<LiveStudentMonitorDTO> data = teacherExamService.getLiveMonitorData(examId, user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Live data fetched", data));
    }

    @PostMapping("/{examId}/broadcast")
    public ResponseEntity<APIResponse<Void>> broadcastMessage(
            @PathVariable Integer examId,
            @Valid @RequestBody MessageRequestDTO dto,
            @AuthenticationPrincipal UserDetails user) {
        teacherExamService.broadcastToExam(examId, user.getUsername(), dto.getMessage());
        return ResponseEntity.ok(new APIResponse<>(200, "Broadcast sent successfully", null));
    }

    @PostMapping("/{examId}/warn/{studentId}")
    public ResponseEntity<APIResponse<Void>> warnStudent(
            @PathVariable Integer examId,
            @PathVariable Integer studentId,
            @Valid @RequestBody MessageRequestDTO dto,
            @AuthenticationPrincipal UserDetails user) {
        teacherExamService.warnStudent(examId, studentId, user.getUsername(), dto.getMessage());
        return ResponseEntity.ok(new APIResponse<>(200, "Warning sent successfully", null));
    }

    @PostMapping("/{examId}/grade/{submissionId}/auto")
    public ResponseEntity<APIResponse<Map<String, Object>>> autoGradePdfSubmission(
            @PathVariable Integer examId, // Kept for URL consistency, even if service uses submissionId
            @PathVariable Integer submissionId,
            @AuthenticationPrincipal UserDetails user) {

        try {
            Map<String, Object> gradingResult = smartGradingService.autoGradeSubmission(submissionId);
            return ResponseEntity.ok(new APIResponse<>(200, "AI Grading successful", gradingResult));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new APIResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new APIResponse<>(500, "An error occurred during smart grading.", null));
        }
    }

    @GetMapping("/pending-gradings")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<APIResponse<List<PendingGradingDTO>>> getPendingGradings(@AuthenticationPrincipal UserDetails user) {
        List<PendingGradingDTO> data = teacherExamService.getPendingPdfGradings(user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Pending gradings fetched", data));
    }


    @PostMapping("/{examId}/grade/{submissionId}/approve")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<APIResponse<Void>> approveGrade(
            @PathVariable Integer examId,
            @PathVariable Integer submissionId,
            @RequestParam java.math.BigDecimal score,
            @RequestBody(required = false) Map<String, Object> payload,
            @AuthenticationPrincipal UserDetails user) {

        // Extract feedback
        String feedback = payload != null && payload.get("comments") != null ? payload.get("comments").toString() : "Manually Graded";

        // Extract AI's calculated score
        java.math.BigDecimal calculatedScore = null;
        if (payload != null && payload.get("aiScore") != null) {
            calculatedScore = new java.math.BigDecimal(payload.get("aiScore").toString());
        }

        // Pass everything to the service
        teacherExamService.approveAndReleaseGrade(user.getUsername(), submissionId, score, calculatedScore, feedback);

        return ResponseEntity.ok(new APIResponse<>(200, "Grade released successfully", null));
    }
}