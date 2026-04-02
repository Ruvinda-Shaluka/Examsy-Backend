package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.dto.response.CalendarExamDTO;
import lk.ijse.examsybackend.dto.response.ClassPeopleDTO;
import lk.ijse.examsybackend.dto.response.StudentClassCardDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StudentDashboardService {
    // The Transactional annotation keeps the DB session open for Lazy loading!
    @Transactional(readOnly = true)
    List<StudentClassCardDTO> getMyEnrolledClasses(String username);

    @Transactional
    void unenrollFromClass(String username, Integer courseId);

    @Transactional
    String joinClass(String username, JoinClassDTO dto);

    @Transactional
    void fileReport(String username, ReportCreateDTO dto);

    @Transactional(readOnly = true)
    ClassPeopleDTO getClassPeople(Integer classId);

    @Transactional(readOnly = true)
    List<CalendarExamDTO> getStudentCalendarExams(String username);
}
