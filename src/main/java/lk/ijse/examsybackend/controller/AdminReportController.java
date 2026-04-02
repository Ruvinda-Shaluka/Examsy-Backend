package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.response.AdminReportDTO;
import lk.ijse.examsybackend.service.AdminReportService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Validated
public class AdminReportController {

    private final AdminReportService adminReportServiceImpl;

    @GetMapping
    public ResponseEntity<APIResponse<List<AdminReportDTO>>> getReports() {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", adminReportServiceImpl.getAllPendingReports()));
    }

    @DeleteMapping("/{reportId}/terminate-class")
    public ResponseEntity<APIResponse<Void>> terminateClass(@PathVariable Integer reportId) {
        adminReportServiceImpl.terminateClass(reportId);
        return ResponseEntity.ok(new APIResponse<>(200, "Class terminated", null));
    }

    @DeleteMapping("/{reportId}/terminate-teacher")
    public ResponseEntity<APIResponse<Void>> terminateTeacher(@PathVariable Integer reportId) {
        adminReportServiceImpl.terminateTeacher(reportId);
        return ResponseEntity.ok(new APIResponse<>(200, "Teacher terminated", null));
    }

    @PutMapping("/{reportId}/dismiss")
    public ResponseEntity<APIResponse<Void>> dismissReport(@PathVariable Integer reportId) {
        adminReportServiceImpl.dismissReport(reportId);
        return ResponseEntity.ok(new APIResponse<>(200, "Report dismissed", null));
    }

    @PostMapping("/{reportId}/warn-teacher")
    public ResponseEntity<APIResponse<Void>> warnTeacher(@PathVariable Integer reportId) {
        adminReportServiceImpl.warnTeacher(reportId);
        return ResponseEntity.ok(new APIResponse<>(200, "Warning sent to teacher.", null));
    }

    @PostMapping("/{reportId}/reply-student")
    public ResponseEntity<APIResponse<Void>> replyToStudent(
            @PathVariable Integer reportId,
            @RequestParam String message) { // Captures the custom message from React
        adminReportServiceImpl.replyToStudent(reportId, message);
        return ResponseEntity.ok(new APIResponse<>(200, "Reply sent to student.", null));
    }
}