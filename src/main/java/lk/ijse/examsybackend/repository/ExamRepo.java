package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamRepo extends JpaRepository<Exam,Integer> {
    List<Exam> findAllByStatus(String published);

    // Change it from OrderByCreatedAtDesc to OrderByIdDesc
    List<Exam> findByCourseIdOrderByIdDesc(Integer classId);

    List<Exam> findByCourseIdAndStatus(Integer courseId, String status);

    // For Teachers: Get exams for courses they teach
    List<Exam> findByCourseTeacherUserAccountUsername(String username);

    // For Students: Get exams for courses they are actively enrolled in
    @Query("SELECT e FROM Exam e JOIN ClassEnrollment ce ON e.course = ce.course WHERE ce.student.userAccount.username = :username")
    List<Exam> findExamsByStudentUsername(@Param("username") String username);

    // Find all published exams for a specific teacher
    List<Exam> findByCourseTeacherUserAccountUsernameAndStatus(String username, String status);

    @Query("SELECT e FROM Exam e WHERE e.course.teacher.userAccount.username = :username AND e.status = 'PUBLISHED' " +
            "AND ((e.examMode IN ('REAL_TIME', 'REAL-TIME') AND e.scheduledStartTime BETWEEN :now AND :deadlineWindow) " +
            "OR (e.examMode = 'DEADLINE' AND e.deadlineTime BETWEEN :now AND :deadlineWindow))")
    List<Exam> findUpcomingExamsForReminders(
            @Param("username") String username,
            @Param("now") java.time.LocalDateTime now,
            @Param("deadlineWindow") java.time.LocalDateTime deadlineWindow);
}
