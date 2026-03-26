package lk.ijse.examsybackend.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AdminDashboardDTO {
    private long totalStudents;
    private long activeTeachers;
    private long pendingReports;
    private long totalUsers;
    private List<ReportDistributionDTO> reportDistribution;
}