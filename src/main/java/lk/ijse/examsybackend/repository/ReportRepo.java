package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.dto.ReportDistributionDTO;
import lk.ijse.examsybackend.entity.Report;
import lk.ijse.examsybackend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReportRepo extends JpaRepository<Report, Integer> {
    // Instantly counts all complaints associated with a specific teacher
    long countByTargetCourseTeacher(Teacher teacher);

    // Count by status (e.g., "PENDING")
    long countByStatus(String status);

    // Group reports by category and count them for the bar chart
    @Query("SELECT new lk.ijse.examsybackend.dto.ReportDistributionDTO(r.category, COUNT(r)) FROM Report r GROUP BY r.category")
    List<ReportDistributionDTO> countReportsByCategory();
}
