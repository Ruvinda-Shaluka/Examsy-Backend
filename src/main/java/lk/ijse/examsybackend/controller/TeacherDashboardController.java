package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.CalendarExamDTO;
import lk.ijse.examsybackend.dto.CourseCreateDTO;
import lk.ijse.examsybackend.dto.TeacherClassCardDTO;
import lk.ijse.examsybackend.service.TeacherDashboardService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/dashboard")
@RequiredArgsConstructor
@Validated
public class TeacherDashboardController {

    private final TeacherDashboardService dashboardService;

    @GetMapping("/classes")
    public ResponseEntity<APIResponse<List<TeacherClassCardDTO>>> getMyClasses(@AuthenticationPrincipal UserDetails userDetails) {
        List<TeacherClassCardDTO> classes = dashboardService.getMyClasses(userDetails.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Classes fetched successfully", classes));
    }

    @PostMapping("/classes")
    public ResponseEntity<APIResponse<TeacherClassCardDTO>> createClass(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CourseCreateDTO dto) {

        TeacherClassCardDTO newClass = dashboardService.createClass(userDetails.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(201, "Class created successfully", newClass));
    }

    @DeleteMapping("/classes/{courseId}")
    public ResponseEntity<APIResponse<String>> deleteClass(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Integer courseId) {
        dashboardService.deleteClass(userDetails.getUsername(), courseId);
        return ResponseEntity.ok(new APIResponse<>(200, "Class deleted successfully", null));
    }

    @PostMapping("/rotate-codes")
    public ResponseEntity<APIResponse<Void>> rotateCodes(@AuthenticationPrincipal UserDetails user) {
        dashboardService.rotateExpiredClassCodes(user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Codes rotated if expired", null));
    }

    @GetMapping("/calendar/exams")
    public ResponseEntity<APIResponse<List<CalendarExamDTO>>> getCalendarExams(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", dashboardService.getTeacherCalendarExams(user.getUsername())));
    }
}