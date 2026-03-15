package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.ClassPeopleDTO;
import lk.ijse.examsybackend.dto.JoinClassDTO;
import lk.ijse.examsybackend.dto.ReportCreateDTO;
import lk.ijse.examsybackend.dto.StudentClassCardDTO;
import lk.ijse.examsybackend.service.StudentDashboardService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/dashboard")
@RequiredArgsConstructor
@Validated
public class StudentDashboardController {

    private final StudentDashboardService dashboardService;

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

    @PostMapping("/classes/join")
    public ResponseEntity<APIResponse<StudentClassCardDTO>> joinClass(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody JoinClassDTO dto) {

        // Calls your custom method that parses the link and returns the new Class Card!
        StudentClassCardDTO joinedClass = dashboardService.joinClass(user.getUsername(), dto);

        return ResponseEntity.ok(new APIResponse<>(200, "Successfully joined the class", joinedClass));
    }

    @PostMapping("/classes/report")
    public ResponseEntity<APIResponse<Void>> reportClass(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReportCreateDTO dto) {

        dashboardService.fileReport(userDetails.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(201, "Report submitted successfully", null));
    }

    @GetMapping("/classes/{classId}/people")
    public ResponseEntity<APIResponse<ClassPeopleDTO>> getClassPeople(@PathVariable Integer classId) {
        ClassPeopleDTO people = dashboardService.getClassPeople(classId);
        return ResponseEntity.ok(new APIResponse<>(200, "Roster loaded", people));
    }
}