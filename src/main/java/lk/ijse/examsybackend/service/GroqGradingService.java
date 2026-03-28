package lk.ijse.examsybackend.service;

import java.util.Map;

public interface GroqGradingService {
    Map<String, Object> evaluateAnswer(String questionText, String modelAnswer, String studentOcrText);
}
