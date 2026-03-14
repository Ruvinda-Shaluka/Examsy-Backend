package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.ExamPublishDTO;
import lk.ijse.examsybackend.dto.ExamSummaryDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TeacherExamService {
    @Transactional
    void publishExam(String username, ExamPublishDTO dto);

    @Transactional(readOnly = true)
    List<ExamSummaryDTO> getClassExams(String username, Integer classId);

    @Transactional
    void deleteExam(String username, Integer examId);
}
