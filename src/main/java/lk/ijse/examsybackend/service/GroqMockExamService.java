package lk.ijse.examsybackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import lk.ijse.examsybackend.entity.MockExam;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface GroqMockExamService {
    @Transactional
    MockExam generateAndSaveExam(String username, String subject, String topic, String difficulty, int count);

    JsonNode gradeShortAnswer(String questionText, String modelAnswer, String studentAnswer, java.math.BigDecimal maxPoints);

    Map<String, Object> evaluateAnswer(String questionText, String modelAnswer, String studentOcrText);
}
