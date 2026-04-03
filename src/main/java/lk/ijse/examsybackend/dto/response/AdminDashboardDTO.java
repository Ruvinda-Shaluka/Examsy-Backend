package lk.ijse.examsybackend.dto.response;
import lk.ijse.examsybackend.dto.nested.ReportDistributionDTO;
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