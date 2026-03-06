package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.ExamPublishDTO;
import lk.ijse.examsybackend.dto.QuestionDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.CourseRepo;
import lk.ijse.examsybackend.repository.ExamRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherExamService {

    private final ExamRepo examRepository;
    private final CourseRepo courseRepository;

    @Transactional
    public void publishExam(String username, ExamPublishDTO dto) {

        // 1. Calculate Total Max Score
        BigDecimal maxScore = BigDecimal.ZERO;
        if (dto.getQuestions() != null) {
            for (QuestionDTO q : dto.getQuestions()) {
                maxScore = maxScore.add(q.getPoints() != null ? q.getPoints() : BigDecimal.ZERO);
            }
        }

        // 2. Loop through every selected class and create the exam
        for (Integer classId : dto.getClassIds()) {
            Course course = courseRepository.findByIdAndTeacherUserAccountUsername(classId, username)
                    .orElseThrow(() -> new RuntimeException("Class not found or unauthorized"));

            Exam exam = Exam.builder()
                    .course(course)
                    .title(dto.getTitle())
                    .examMode(dto.getExamMode())
                    .examType(dto.getExamType())
                    .scheduledStartTime(dto.getScheduledStartTime())
                    .deadlineTime(dto.getDeadlineTime())
                    .durationMinutes(dto.getDurationMinutes())
                    .pdfResourceUrl(dto.getPdfResourceUrl())
                    .maxScore(maxScore)
                    .status("PUBLISHED")
                    .build();

            // 3. Map Questions (if it's MCQ or SHORT)
            if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
                List<Question> mappedQuestions = new ArrayList<>();
                int orderIdx = 1;

                for (QuestionDTO qDto : dto.getQuestions()) {
                    Question question = Question.builder()
                            .exam(exam)
                            .questionText(qDto.getQuestionText())
                            .points(qDto.getPoints())
                            .orderIndex(orderIdx++)
                            .build();

                    // 4. Map Options (if it's an MCQ)
                    if (qDto.getOptions() != null && !qDto.getOptions().isEmpty()) {
                        List<QuestionOption> options = new ArrayList<>();
                        for (int i = 0; i < qDto.getOptions().size(); i++) {
                            options.add(QuestionOption.builder()
                                    .question(question)
                                    .optionText(qDto.getOptions().get(i))
                                    .isCorrect(qDto.getCorrectOptionIndex() != null && qDto.getCorrectOptionIndex() == i)
                                    .build());
                        }
                        question.setOptions(options);
                    }
                    mappedQuestions.add(question);
                }
                exam.setQuestions(mappedQuestions);
            }

            // Save everything cascaded!
            examRepository.save(exam);
        }
    }
}