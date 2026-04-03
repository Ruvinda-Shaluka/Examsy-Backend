package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.dto.response.ExamAnalyticsDTO;
import lk.ijse.examsybackend.dto.response.ExamSummaryDTO;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

public interface TeacherExamService {
    @Transactional
    void publishExam(String username, ExamPublishDTO dto);

    @Transactional(readOnly = true)
    List<ExamSummaryDTO> getClassExams(String username, Integer classId);

    @Transactional
    void deleteExam(String username, Integer examId);

    @Transactional
    void updateExamTiming(String username, Integer examId, UpdateExamDeadlineDTO dto);

    @Transactional(readOnly = true)
    OngoingExamGroupDTO getOngoingExams(String teacherUsername);

    @Transactional(readOnly = true)
    List<LiveStudentMonitorDTO> getLiveMonitorData(Integer examId, String teacherUsername);

    @Transactional
    void broadcastToExam(Integer examId, String teacherUsername, String message);

    @Transactional
    void warnStudent(Integer examId, Integer studentId, String teacherUsername, String message);

    @Transactional(readOnly = true)
    List<PendingGradingDTO> getPendingPdfGradings(String teacherUsername);

    @Transactional
    void approveAndReleaseGrade(String teacherUsername, Integer submissionId, BigDecimal finalScore, BigDecimal calculatedScore, String feedback);

    @Transactional
    void triggerUpcomingExamReminders(String teacherUsername);

    @Transactional(readOnly = true)
    ExamAnalyticsDTO getExamAnalytics(Integer examId, String teacherUsername);
}
