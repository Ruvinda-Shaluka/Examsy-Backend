package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.StudentClassCardDTO;
import lk.ijse.examsybackend.service.ClassEnrollmentService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/dashboard")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final ClassEnrollmentService dashboardService;

    @GetMapping("/classes")
    public ResponseEntity<APIResponse<List<StudentClassCardDTO>>> getEnrolledClasses(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<StudentClassCardDTO> classes = dashboardService.getMyEnrolledClasses(userDetails.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Classes fetched successfully", classes));
    }

    @DeleteMapping("/classes/{courseId}/unenroll")
    public ResponseEntity<APIResponse<String>> unenrollFromClass(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer courseId) {
        dashboardService.unenrollFromClass(userDetails.getUsername(), courseId);
        return ResponseEntity.ok(new APIResponse<>(200, "Successfully unenrolled from class", null));
    }
}