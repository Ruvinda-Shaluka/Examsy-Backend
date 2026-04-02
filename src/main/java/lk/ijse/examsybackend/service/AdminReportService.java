package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.response.AdminReportDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AdminReportService {
    @Transactional(readOnly = true)
    List<AdminReportDTO> getAllPendingReports();

    @Transactional
    void terminateClass(Integer reportId);

    // Send a direct custom reply to the student
    @Transactional
    void replyToStudent(Integer reportId, String messageBody);

    @Transactional
    void terminateTeacher(Integer reportId);

    @Transactional
    void dismissReport(Integer reportId);

    // Send an official warning to the teacher AND acknowledge the student
    @Transactional
    void warnTeacher(Integer reportId);
}
