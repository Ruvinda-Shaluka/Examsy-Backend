package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.ExamPublishDTO;
import lk.ijse.examsybackend.dto.ExamSummaryDTO;
import lk.ijse.examsybackend.dto.QuestionDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.CourseRepo;
import lk.ijse.examsybackend.repository.ExamRepo;
import lk.ijse.examsybackend.service.TeacherExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherExamServiceImpl implements TeacherExamService {

    private final ExamRepo examRepository;
    private final CourseRepo courseRepository;

    @Transactional
    @Override
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
                            .questionType(dto.getExamType())
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

    @Transactional(readOnly = true)
    @Override
    public List<ExamSummaryDTO> getClassExams(String username, Integer classId) {
        // Security check
        Course course = courseRepository.findByIdAndTeacherUserAccountUsername(classId, username)
                .orElseThrow(() -> new RuntimeException("Class not found or unauthorized"));

        // Fetch exams linked to this course
        List<Exam> exams = examRepository.findByCourseIdOrderByIdDesc(classId);

        return exams.stream().map(e -> ExamSummaryDTO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .examType(e.getExamType())
                .status(e.getStatus())
                .deadlineTime(e.getDeadlineTime())
                .durationMinutes(e.getDurationMinutes())
                .maxScore(e.getMaxScore())
                .build()
        ).toList();
    }

    @Transactional
    @Override
    public void deleteExam(String username, Integer examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to delete this exam");
        }

        // Because of JPA Cascading, deleting the exam will auto-delete its questions and options!
        examRepository.delete(exam);
    }

    @Transactional
    @Override
    public void updateExamDeadline(String username, Integer examId, LocalDateTime newDeadline) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to update this exam");
        }

        exam.setDeadlineTime(newDeadline);
        examRepository.save(exam);
    }
}