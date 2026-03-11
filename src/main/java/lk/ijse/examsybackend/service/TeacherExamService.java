package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.ExamPublishDTO;
import org.springframework.transaction.annotation.Transactional;

public interface TeacherExamService {
    @Transactional
    void publishExam(String username, ExamPublishDTO dto);
}
