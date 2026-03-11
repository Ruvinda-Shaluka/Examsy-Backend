package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.JoinClassDTO;
import lk.ijse.examsybackend.dto.ReportCreateDTO;
import lk.ijse.examsybackend.dto.StudentClassCardDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StudentDashboardService {
    // The Transactional annotation keeps the DB session open for Lazy loading!
    @Transactional(readOnly = true)
    List<StudentClassCardDTO> getMyEnrolledClasses(String username);

    @Transactional
    void unenrollFromClass(String username, Integer courseId);

    @Transactional
    StudentClassCardDTO joinClass(String username, JoinClassDTO dto);

    @Transactional
    void fileReport(String username, ReportCreateDTO dto);
}
