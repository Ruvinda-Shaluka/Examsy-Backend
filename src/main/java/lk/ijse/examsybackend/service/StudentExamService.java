package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamService {

    private final ExamRepo examRepository;
    private final ExamSubmissionRepo submissionRepository;
    private final SubmissionAnswerRepo answerRepository;
    private final QuestionOptionRepo optionRepository;
    private final StudentRepo studentRepository;
    private final ProctoringLogRepo proctoringLogRepository;

    // --- 1. PROCTORING: Track Cheating / Tab Switches ---
    @Transactional
    public void logSecurityViolation(String username, ProctoringDTO dto) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        ExamSubmission submission = submissionRepository.findByExamIdAndStudentId(dto.getExamId(), student.getId())
                .orElseThrow(() -> new RuntimeException("Active exam session not found"));

        submission.setSuspiciousEventCount(submission.getSuspiciousEventCount() + 1);
        submission.setTotalTimeAwaySeconds(submission.getTotalTimeAwaySeconds() + dto.getDurationSeconds());
        submission.setProctoringStatus("FLAGGED");
        submission.setLastKnownAction(dto.getEventType());

        submissionRepository.save(submission);

        ProctoringLog log = ProctoringLog.builder()
                .examSubmission(submission)
                .eventType(dto.getEventType())
                .durationSeconds(dto.getDurationSeconds())
                .build();

        proctoringLogRepository.save(log);
    }

    // --- 2. EXAM START: Get Exam Data for React (Without Correct Answers!) ---
    @Transactional
    public StudentExamViewDTO getExamForStudent(String username, Integer examId) {
        Student student = studentRepository.findByUserAccountUsername(username).orElseThrow();
        Exam exam = examRepository.findById(examId).orElseThrow();

        // Start the timer by creating a submission if they haven't started yet
        ExamSubmission submission = submissionRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseGet(() -> {
                    ExamSubmission newSub = ExamSubmission.builder()
                            .exam(exam)
                            .student(student)
                            .actualStartTime(LocalDateTime.now())
                            .status("IN_PROGRESS")
                            .suspiciousEventCount(0)
                            .totalTimeAwaySeconds(0)
                            .proctoringStatus("SECURE")
                            .build();
                    return submissionRepository.save(newSub);
                });

        if ("SUBMITTED".equals(submission.getStatus())) {
            throw new RuntimeException("You have already submitted this exam.");
        }

        // Map questions but STRIP OUT the correct answers
        List<StudentQuestionViewDTO> questionDTOs = exam.getQuestions().stream().map(q ->
                StudentQuestionViewDTO.builder()
                        .id(q.getId())
                        .text(q.getQuestionText())
                        .type(q.getQuestionType())
                        .options(q.getOptions() != null ? q.getOptions().stream().map(o ->
                                StudentOptionViewDTO.builder().id(o.getId()).text(o.getOptionText()).build()
                        ).collect(Collectors.toList()) : null)
                        .build()
        ).collect(Collectors.toList());

        return StudentExamViewDTO.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .examType(exam.getExamType())
                .durationMinutes(exam.getDurationMinutes())
                .pdfResourceUrl(exam.getPdfResourceUrl())
                .questions(questionDTOs)
                .build();
    }

    // --- 3. EXAM SUBMIT: Grade Answers Instantly ---
    @Transactional
    public ExamResultDTO submitExam(String username, Integer examId, ExamSubmitDTO dto) {
        Student student = studentRepository.findByUserAccountUsername(username).orElseThrow();
        Exam exam = examRepository.findById(examId).orElseThrow();

        ExamSubmission submission = submissionRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if ("SUBMITTED".equals(submission.getStatus())) {
            throw new RuntimeException("Already submitted!");
        }

        BigDecimal totalEarnedScore = BigDecimal.ZERO;

        if (dto.getAnswers() != null) {
            for (AnswerSubmitDTO ansDto : dto.getAnswers()) {
                Question question = exam.getQuestions().stream()
                        .filter(q -> q.getId().equals(ansDto.getQuestionId())).findFirst().orElseThrow();

                SubmissionAnswer answerRecord = SubmissionAnswer.builder()
                        .submission(submission)
                        .question(question)
                        .answerText(ansDto.getAnswerText())
                        .build();

                // Auto-Grade MCQ logic
                if ("MCQ".equalsIgnoreCase(exam.getExamType()) && ansDto.getSelectedOptionId() != null) {
                    QuestionOption selectedOpt = optionRepository.findById(ansDto.getSelectedOptionId()).orElse(null);
                    answerRecord.setSelectedOption(selectedOpt);

                    if (selectedOpt != null && selectedOpt.getIsCorrect()) {
                        BigDecimal points = question.getPoints() != null ? question.getPoints() : BigDecimal.ONE;
                        answerRecord.setScoreAwarded(points);
                        totalEarnedScore = totalEarnedScore.add(points);
                    } else {
                        answerRecord.setScoreAwarded(BigDecimal.ZERO);
                    }
                }
                answerRepository.save(answerRecord);
            }
        }

        // Finalize Submission
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("SUBMITTED");
        submission.setPdfSubmissionUrl(dto.getPdfSubmissionUrl());

        if ("MCQ".equalsIgnoreCase(exam.getExamType())) {
            submission.setCalculatedScore(totalEarnedScore);
            submission.setFinalScore(totalEarnedScore); // Score is locked in!
        }

        submissionRepository.save(submission);

        return ExamResultDTO.builder()
                .score(totalEarnedScore)
                .maxScore(exam.getMaxScore())
                .status("MCQ".equalsIgnoreCase(exam.getExamType()) ? "GRADED" : "PENDING_TEACHER_REVIEW")
                .message("Successfully submitted!")
                .build();
    }

    // --- 4. ACADEMIC VAULT: Fetch published exams for the dashboard ---
    @Transactional(readOnly = true)
    public VaultExamsResponseDTO getVaultExams(String username) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Exam> allPublishedExams = examRepository.findAllByStatus("PUBLISHED");

        List<VaultExamItemDTO> upcoming = new java.util.ArrayList<>();
        List<VaultExamItemDTO> available = new java.util.ArrayList<>();

        for (Exam exam : allPublishedExams) {

            // 🟢 CHECK SUBMISSION STATUS FOR THIS SPECIFIC STUDENT
            String currentStatus = "NOT_STARTED";
            java.util.Optional<ExamSubmission> subOpt = submissionRepository.findByExamIdAndStudentId(exam.getId(), student.getId());
            if (subOpt.isPresent()) {
                currentStatus = subOpt.get().getStatus(); // Gets "SUBMITTED" or "IN_PROGRESS"
            }

            VaultExamItemDTO dto = VaultExamItemDTO.builder()
                    .id(exam.getId())
                    .title(exam.getTitle())
                    .examType(exam.getExamType())
                    .durationMinutes(exam.getDurationMinutes())
                    .scheduledStartTime(exam.getScheduledStartTime())
                    .deadlineTime(exam.getDeadlineTime())
                    .status(exam.getStatus())
                    .studentStatus(currentStatus) // 🟢 Map the status to the DTO
                    .build();

            if ("REAL-TIME".equalsIgnoreCase(exam.getExamMode()) || "REAL_TIME".equalsIgnoreCase(exam.getExamMode())) {
                upcoming.add(dto);
            } else {
                available.add(dto);
            }
        }

        return new VaultExamsResponseDTO(upcoming, available);
    }
}