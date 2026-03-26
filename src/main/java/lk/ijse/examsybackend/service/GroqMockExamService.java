package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.entity.MockExam;
import org.springframework.transaction.annotation.Transactional;

public interface GroqMockExamService {
    @Transactional
    MockExam generateAndSaveExam(String username, String subject, String topic, String difficulty, int count);
}
