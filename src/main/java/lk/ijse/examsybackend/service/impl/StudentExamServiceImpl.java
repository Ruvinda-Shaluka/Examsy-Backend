package lk.ijse.examsybackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.GroqMockExamService;
import lk.ijse.examsybackend.service.StudentExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentExamServiceImpl implements StudentExamService {

    private final ExamRepo examRepository;
    private final ExamSubmissionRepo submissionRepository;
    private final SubmissionAnswerRepo answerRepository;
    private final QuestionOptionRepo optionRepository;
    private final StudentRepo studentRepository;
    private final ProctoringLogRepo proctoringLogRepository;
    private final GroqMockExamService groqMockExamService;

    // --- 1. PROCTORING: Track Cheating / Tab Switches ---
    @Transactional
    @Override
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
    @Override
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
    @Override
    public ExamResultDTO submitExam(String username, Integer examId, ExamSubmitDTO dto) {
        Student student = studentRepository.findByUserAccountUsername(username).orElseThrow();
        Exam exam = examRepository.findById(examId).orElseThrow();

        ExamSubmission submission = submissionRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        if ("SUBMITTED".equals(submission.getStatus()) || "GRADED".equals(submission.getStatus())) {
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

                // AUTO-GRADE MCQ
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
                // AUTO-GRADE SHORT ANSWER VIA GROQ
                else if ("SHORT".equalsIgnoreCase(exam.getExamType()) && ansDto.getAnswerText() != null) {
                    BigDecimal maxPoints = question.getPoints() != null ? question.getPoints() : BigDecimal.valueOf(5);

                    try {
                        JsonNode aiResult = groqMockExamService.gradeShortAnswer(
                                question.getQuestionText(),
                                question.getModelAnswer(),
                                ansDto.getAnswerText(),
                                maxPoints
                        );

                        BigDecimal awardedScore = BigDecimal.valueOf(aiResult.path("awarded_score").asDouble());
                        String feedback = aiResult.path("feedback").asText();

                        if (awardedScore.compareTo(maxPoints) > 0) awardedScore = maxPoints;
                        if (awardedScore.compareTo(BigDecimal.ZERO) < 0) awardedScore = BigDecimal.ZERO;

                        answerRecord.setScoreAwarded(awardedScore);
                        answerRecord.setFeedback(feedback);
                        totalEarnedScore = totalEarnedScore.add(awardedScore);

                    } catch (Exception e) {
                        answerRecord.setScoreAwarded(BigDecimal.ZERO);
                        answerRecord.setFeedback("AI Grading Failed. Pending manual teacher review.");
                    }
                }

                answerRepository.save(answerRecord);
            }
        }

        // Finalize Submission Status and URL
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setPdfSubmissionUrl(dto.getPdfSubmissionUrl());

        String finalGrade = "N/A";
        BigDecimal percentage = BigDecimal.ZERO;
        String finalStatus = "PENDING_TEACHER_REVIEW";

        // Calculate Grades for Auto-Graded Exams (MCQ and SHORT)
        if ("MCQ".equalsIgnoreCase(exam.getExamType()) || "SHORT".equalsIgnoreCase(exam.getExamType())) {
            finalStatus = "GRADED";

            // Set the exact score to both Calculated (AI) and Final (Teacher equivalent)
            submission.setCalculatedScore(totalEarnedScore);
            submission.setFinalScore(totalEarnedScore);

            // Calculate Percentage
            if (exam.getMaxScore() != null && exam.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
                percentage = totalEarnedScore.divide(exam.getMaxScore(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            // Determine Examsy Grade Letter
            double pct = percentage.doubleValue();
            if (pct < 40) finalGrade = "F";
            else if (pct < 55) finalGrade = "S";
            else if (pct < 65) finalGrade = "C";
            else if (pct < 75) finalGrade = "B";
            else finalGrade = "A";

            // Explicitly save the newly calculated grade letter to the database!
            submission.setAwardedGradeLetter(finalGrade);
        }

        submission.setStatus(finalStatus);
        submissionRepository.save(submission);

        // Return the payload back to React to power the SubmitModal UI
        return ExamResultDTO.builder()
                .score(totalEarnedScore)
                .maxScore(exam.getMaxScore())
                .percentage(percentage.setScale(1, RoundingMode.HALF_UP))
                .grade(finalGrade)
                .status(finalStatus)
                .message("Successfully submitted and graded!")
                .build();
    }

    // --- 4. ACADEMIC VAULT: Fetch published exams for the dashboard ---
    @Transactional(readOnly = true)
    @Override
    public VaultExamsResponseDTO getVaultExams(String username, Integer classId) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Fetch ONLY exams for this specific class!
        List<Exam> classExams = examRepository.findByCourseIdAndStatus(classId, "PUBLISHED");

        List<VaultExamItemDTO> upcoming = new java.util.ArrayList<>();
        List<VaultExamItemDTO> available = new java.util.ArrayList<>();

        for (Exam exam : classExams) {

            String currentStatus = "NOT_STARTED";
            boolean hasFinished = false;

            java.util.Optional<ExamSubmission> subOpt = submissionRepository.findByExamIdAndStudentId(exam.getId(), student.getId());

            if (subOpt.isPresent()) {
                ExamSubmission submission = subOpt.get();
                currentStatus = submission.getStatus();

                // Check if the record actually has a final score or graded letter
                if (submission.getFinalScore() != null || submission.getAwardedGradeLetter() != null) {
                    hasFinished = true;
                }
                // Fallback for PDF exams that are submitted but waiting for the teacher to grade them
                else if ("SUBMITTED".equalsIgnoreCase(currentStatus)) {
                    hasFinished = true;
                }
            }

            // If they have a grade, skip sending this exam to the frontend vault
            if (hasFinished) {
                continue;
            }

            VaultExamItemDTO dto = VaultExamItemDTO.builder()
                    .id(exam.getId())
                    .title(exam.getTitle())
                    .examType(exam.getExamType())
                    .durationMinutes(exam.getDurationMinutes())
                    .scheduledStartTime(exam.getScheduledStartTime())
                    .deadlineTime(exam.getDeadlineTime())
                    .status(exam.getStatus())
                    .studentStatus(currentStatus)
                    .build();

            if ("REAL-TIME".equalsIgnoreCase(exam.getExamMode()) || "REAL_TIME".equalsIgnoreCase(exam.getExamMode())) {
                upcoming.add(dto);
            } else {
                available.add(dto);
            }
        }

        return new VaultExamsResponseDTO(upcoming, available);
    }

    @Transactional
    @Override
    public ProctoringStatsDTO logProctoringEvent(Integer examId, String studentUsername, ProctoringLogDTO dto) {
        Student student = studentRepository.findByUserAccountUsername(studentUsername)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Find their active exam submission
        ExamSubmission submission = submissionRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new RuntimeException("Active exam session not found."));

        // 1. Save the specific log
        ProctoringLog log = ProctoringLog.builder()
                .examSubmission(submission)
                .eventType(dto.getEventType())
                .durationSeconds(dto.getDurationSeconds())
                .build();
        proctoringLogRepository.save(log);

        // 2. Update Cumulative Totals
        int newFlagCount = (submission.getSuspiciousEventCount() == null ? 0 : submission.getSuspiciousEventCount()) + 1;
        int newTotalTime = (submission.getTotalTimeAwaySeconds() == null ? 0 : submission.getTotalTimeAwaySeconds()) + dto.getDurationSeconds();

        submission.setSuspiciousEventCount(newFlagCount);
        submission.setTotalTimeAwaySeconds(newTotalTime);
        submission.setProctoringStatus("SUSPICIOUS"); // Flag them for the teacher monitor!

        submissionRepository.save(submission);

        // 3. Return the new totals so the frontend modal can display them accurately
        return ProctoringStatsDTO.builder()
                .totalFlags(newFlagCount)
                .totalAwaySeconds(newTotalTime)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public StudentAnalyticsDTO getStudentAnalytics(String username) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Fetch all completed & graded exams, sorted chronologically
        List<ExamSubmission> submissions = submissionRepository.findByStudentIdAndStatusOrderBySubmittedAtAsc(student.getId(), "GRADED");

        if (submissions.isEmpty()) {
            return StudentAnalyticsDTO.builder()
                    .gpa("0.0")
                    .bestScore("N/A")
                    .bestExam("No data")
                    .lowestScore("N/A")
                    .lowestExam("No data")
                    .rankText("N/A")
                    .rankSubText("Take an exam first")
                    .chartData(new java.util.ArrayList<>())
                    .build();
        }

        double highestPct = -1.0;
        double lowestPct = 101.0;
        String bestExamTitle = "";
        String lowestExamTitle = "";
        double totalGpaPoints = 0.0;

        List<ExamChartDataDTO> chartData = new java.util.ArrayList<>();

        for (ExamSubmission sub : submissions) {
            java.math.BigDecimal finalScore = sub.getFinalScore() != null ? sub.getFinalScore() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal maxScore = sub.getExam().getMaxScore();

            double pct = 0.0;
            if (maxScore != null && maxScore.compareTo(java.math.BigDecimal.ZERO) > 0) {
                pct = finalScore.divide(maxScore, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
            }

            // Track Best Score
            if (pct > highestPct) {
                highestPct = pct;
                bestExamTitle = sub.getExam().getTitle();
            }

            // Track Lowest Score
            if (pct < lowestPct) {
                lowestPct = pct;
                lowestExamTitle = sub.getExam().getTitle();
            }

            // Map Examsy Letter Grade to Standard 4.0 GPA Scale
            String grade = sub.getAwardedGradeLetter();
            if ("A".equals(grade)) totalGpaPoints += 4.0;
            else if ("B".equals(grade)) totalGpaPoints += 3.0;
            else if ("C".equals(grade)) totalGpaPoints += 2.0;
            else if ("S".equals(grade)) totalGpaPoints += 1.0;
            // 'F' gets 0.0

            // Add to Chart Data
            chartData.add(ExamChartDataDTO.builder()
                    .exam(sub.getExam().getTitle())
                    .score(Math.round(pct * 10.0) / 10.0) // Round to 1 decimal place
                    .build());
        }

        double calculatedGpa = totalGpaPoints / submissions.size();

        return StudentAnalyticsDTO.builder()
                .gpa(String.format("%.2f", calculatedGpa))
                .bestScore(String.format("%.0f%%", highestPct))
                .bestExam(bestExamTitle)
                .lowestScore(String.format("%.0f%%", lowestPct))
                .lowestExam(lowestExamTitle)
                .rankText(submissions.size() + "")
                .rankSubText("Exams Completed")
                .chartData(chartData)
                .build();
    }

}