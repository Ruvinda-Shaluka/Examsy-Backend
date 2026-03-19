package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.ExamPublishDTO;
import lk.ijse.examsybackend.dto.ExamSummaryDTO;
import lk.ijse.examsybackend.dto.OngoingExamGroupDTO;
import lk.ijse.examsybackend.dto.UpdateExamDeadlineDTO;
import org.springframework.transaction.annotation.Transactional;

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
}
