package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Report;
import lk.ijse.examsybackend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepo extends JpaRepository<Report, Integer> {
    // Instantly counts all complaints associated with a specific teacher
    long countByTargetCourseTeacher(Teacher teacher);
}
