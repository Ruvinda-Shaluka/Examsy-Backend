package lk.ijse.examsybackend.service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface SmartGradingService {
    @Transactional(readOnly = true)
    Map<String, Object> autoGradeSubmission(Integer submissionId);
}
