package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.AdminDashboardDTO;
import lk.ijse.examsybackend.dto.ReportDistributionDTO;
import lk.ijse.examsybackend.entity.Role;
import lk.ijse.examsybackend.repository.ReportRepo;
import lk.ijse.examsybackend.repository.UserAccountRepo;
import lk.ijse.examsybackend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserAccountRepo userAccountRepository;
    private final ReportRepo reportRepository;

    public AdminDashboardDTO getDashboardMetrics() {
        long totalStudents = userAccountRepository.countByRoleAndIsActiveTrue(Role.STUDENT);
        long activeTeachers = userAccountRepository.countByRoleAndIsActiveTrue(Role.TEACHER);
        long totalUsers = userAccountRepository.count();
        long pendingReports = reportRepository.countByStatus("PENDING");

        List<ReportDistributionDTO> distribution = reportRepository.countReportsByCategory();

        return AdminDashboardDTO.builder()
                .totalStudents(totalStudents)
                .activeTeachers(activeTeachers)
                .pendingReports(pendingReports)
                .totalUsers(totalUsers)
                .reportDistribution(distribution)
                .build();
    }
}