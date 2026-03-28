package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.entity.ExamSubmission;
import lk.ijse.examsybackend.entity.Question;
import lk.ijse.examsybackend.repository.ExamSubmissionRepo;
import lk.ijse.examsybackend.service.GroqGradingService;
import lk.ijse.examsybackend.service.GroqMockExamService;
import lk.ijse.examsybackend.service.OCRService;
import lk.ijse.examsybackend.service.SmartGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartGradingServiceImpl implements SmartGradingService {

    private final ExamSubmissionRepo submissionRepository;
    private final OCRService ocrService;
    private final GroqGradingService groqGradingService;

    @Transactional(readOnly = true)
    @Override
    public Map<String, Object> autoGradeSubmission(Integer submissionId) {
        // 1. Fetch the student's submission from the database
        ExamSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found with ID: " + submissionId));

        if (submission.getPdfSubmissionUrl() == null || submission.getPdfSubmissionUrl().isEmpty()) {
            throw new IllegalStateException("No PDF found for this submission.");
        }

        // 2. Aggregate all questions and model answers into a single rubric string
        StringBuilder questionsRubric = new StringBuilder();
        StringBuilder modelAnswersRubric = new StringBuilder();

        int questionNumber = 1;
        for (Question q : submission.getExam().getQuestions()) {
            questionsRubric.append("Q").append(questionNumber).append(": ").append(q.getQuestionText()).append("\n");
            modelAnswersRubric.append("A").append(questionNumber).append(": ").append(q.getModelAnswer()).append("\n\n");
            questionNumber++;
        }

        // 3. Extract handwriting from the PDF
        String studentOcrText = ocrService.extractTextFromPdfUrl(submission.getPdfSubmissionUrl());

        System.out.println("\n========== OCR EXTRACTED TEXT ==========");
        System.out.println(studentOcrText);
        System.out.println("========================================\n");

        // 4. Send to Groq Llama 3 for evaluation
        Map<String, Object> aiResult = groqGradingService.evaluateAnswer(
                questionsRubric.toString().trim(),
                modelAnswersRubric.toString().trim(),
                studentOcrText
        );

        // 5. Append the Exam's Max Score so the React frontend can display "85 / 100"
        aiResult.put("maxScore", submission.getExam().getMaxScore());

        return aiResult;
    }
}