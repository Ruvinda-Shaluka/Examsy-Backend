package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.NotificationDTO;
import lk.ijse.examsybackend.entity.Course;
import lk.ijse.examsybackend.entity.Exam;
import lk.ijse.examsybackend.entity.ExamSubmission;
import lk.ijse.examsybackend.entity.Student;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationService {
    @Transactional(readOnly = true)
    List<NotificationDTO> getMyNotifications(String username);

    @Transactional(readOnly = true)
    long getUnreadCount(String username);

    @Transactional
    void markAsRead(Integer notificationId, String username);

    @Transactional
    void markAllAsRead(String username);

    @Transactional
    void dispatchAnnouncementNotifications(Integer courseId, String courseName, String teacherName, String content, boolean isUpdate);

    void notifyTeacherOfJoinRequest(Course course, Student student);

    @Transactional
    void notifyStudentOfJoinResult(Student student, Course course, boolean isApproved);

    @Transactional
    void dispatchExamBroadcast(Integer examId, String teacherName, String courseName, String messageContent);

    @Transactional
    void dispatchStudentWarning(Integer studentId, String teacherName, String courseName, String messageContent);

    @Transactional
    void dispatchNewExamNotification(Exam exam, Course course, String teacherName);

    @Transactional
    void notifyStudentOfGradedExam(ExamSubmission submission, Course course, String teacherName, java.math.BigDecimal finalScore);
}
