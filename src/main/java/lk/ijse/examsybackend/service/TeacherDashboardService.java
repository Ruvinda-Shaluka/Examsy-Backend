package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.response.CalendarExamDTO;
import lk.ijse.examsybackend.dto.request.CourseCreateDTO;
import lk.ijse.examsybackend.dto.TeacherClassCardDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TeacherDashboardService {
    @Transactional(readOnly = true)
    List<TeacherClassCardDTO> getMyClasses(String username);

    @Transactional
    void deleteClass(String username, Integer courseId);

    @Transactional
    TeacherClassCardDTO createClass(String username, CourseCreateDTO dto);

    @Transactional
    void rotateExpiredClassCodes(String username);

    @Transactional(readOnly = true)
    List<CalendarExamDTO> getTeacherCalendarExams(String username);
}
