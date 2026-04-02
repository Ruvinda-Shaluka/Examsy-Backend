package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.response.AdminDashboardDTO;
import lk.ijse.examsybackend.service.AdminDashboardService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard/metrics")
    public ResponseEntity<APIResponse<AdminDashboardDTO>> getMetrics() {
        AdminDashboardDTO metrics = adminDashboardService.getDashboardMetrics();
        return ResponseEntity.ok(new APIResponse<>(200, "Metrics Fetched Successfully", metrics));
    }
}