package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.NotificationService;
import lk.ijse.examsybackend.service.TeacherExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherExamServiceImpl implements TeacherExamService {

    private final ExamRepo examRepository;
    private final CourseRepo courseRepository;
    private final ClassEnrollmentRepo classEnrollmentRepo;
    private final ExamSubmissionRepo examSubmissionRepo;
    private final ProctoringLogRepo proctoringLogRepo;
    private final NotificationService notificationService;


    @Transactional
    @Override
    public void publishExam(String username, ExamPublishDTO dto) {

        // 1. Calculate Total Max Score Dynamically
        BigDecimal maxScore = BigDecimal.ZERO;

        if ("PDF".equalsIgnoreCase(dto.getExamType())) {
            // PDF: Uses manual user input from the frontend
            maxScore = dto.getMaxScore() != null ? dto.getMaxScore() : BigDecimal.ZERO;
        } else if (dto.getQuestions() != null && !dto.getQuestions().isEmpty()) {
            // MCQ: Automatically 1 point per question
            if ("MCQ".equalsIgnoreCase(dto.getExamType())) {
                maxScore = BigDecimal.valueOf(dto.getQuestions().size());
            }
            // SHORT: Sum of individual question points (defaults to 1 point each if missing)
            else if ("SHORT".equalsIgnoreCase(dto.getExamType())) {
                for (QuestionDTO q : dto.getQuestions()) {
                    maxScore = maxScore.add(q.getPoints() != null ? q.getPoints() : BigDecimal.ONE);
                }
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

                    // Ensure the individual question points match the logic we used for the maxScore
                    BigDecimal qPoints = qDto.getPoints();
                    if ("MCQ".equalsIgnoreCase(dto.getExamType())) {
                        qPoints = BigDecimal.ONE;
                    } else if (qPoints == null) {
                        qPoints = BigDecimal.ONE;
                    }

                    Question question = Question.builder()
                            .exam(exam)
                            .questionText(qDto.getQuestionText())
                            .questionType(dto.getExamType())
                            .points(qPoints)
                            .modelAnswer(qDto.getModelAnswer())
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

            // Trigger Notifications to the Class!
            notificationService.dispatchNewExamNotification(exam, course, course.getTeacher().getFullName());
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
                .scheduledStartTime(e.getScheduledStartTime())
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
    public void updateExamTiming(String username, Integer examId, UpdateExamDeadlineDTO dto) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to update this exam");
        }

        // Update all three timing properties
        exam.setScheduledStartTime(dto.getScheduledStartTime());
        exam.setDeadlineTime(dto.getDeadlineTime());
        exam.setDurationMinutes(dto.getDurationMinutes());

        examRepository.save(exam);
    }

    @Transactional(readOnly = true)
    @Override
    public OngoingExamGroupDTO getOngoingExams(String teacherUsername) {
        // Fetch all "PUBLISHED" exams belonging to this teacher
        List<Exam> allExams = examRepository.findByCourseTeacherUserAccountUsernameAndStatus(teacherUsername, "PUBLISHED");

        List<OngoingExamDTO> realTimeList = new ArrayList<>();
        List<OngoingExamDTO> deadlineList = new ArrayList<>();


        for (Exam exam : allExams) {
            // Calculate Stats
            int totalStudents = classEnrollmentRepo.countByCourseId(exam.getCourse().getId());
            int activeStudents = examSubmissionRepo.countByExamIdAndStatusIn(exam.getId(), List.of("IN_PROGRESS", "ACTIVE"));
            int submissions = examSubmissionRepo.countByExamIdAndStatusIn(exam.getId(), List.of("COMPLETED", "SUBMITTED"));

            OngoingExamDTO dto = OngoingExamDTO.builder()
                    .id(exam.getId())
                    .title(exam.getTitle())
                    .className(exam.getCourse().getName() + " - " + exam.getCourse().getSectionName())
                    .examMode(exam.getExamMode())
                    .activeStudents(activeStudents)
                    .submissions(submissions)
                    .totalStudents(totalStudents)
                    .build();

            String mode = exam.getExamMode();

            if (mode != null && (mode.equalsIgnoreCase("REAL_TIME") || mode.equalsIgnoreCase("REAL-TIME"))) {
                // Handle Real-Time Exams
                if (exam.getScheduledStartTime() != null && exam.getDurationMinutes() != null) {
                    LocalDateTime endTime = exam.getScheduledStartTime().plusMinutes(exam.getDurationMinutes());
                    long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), endTime);
                    dto.setRemainingTime(minutesLeft > 0 ? minutesLeft + "m left" : "Ending soon");
                } else {
                    dto.setRemainingTime("Time TBA");
                }
                realTimeList.add(dto);

            } else {
                // Handle Deadline-Based Exams (Fallback for any other mode, like "DEADLINE")
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                dto.setDeadline(exam.getDeadlineTime() != null ? exam.getDeadlineTime().format(formatter) : "No Deadline");
                deadlineList.add(dto);
            }
        }

        return OngoingExamGroupDTO.builder()
                .realTime(realTimeList)
                .deadline(deadlineList)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public List<LiveStudentMonitorDTO> getLiveMonitorData(Integer examId, String teacherUsername) {
        // 1. Security Check
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized: You do not own this exam.");
        }

        // 2. Get ALL students enrolled in the class, regardless of exam status
        List<ClassEnrollment> enrollments = classEnrollmentRepo.findByCourseId(exam.getCourse().getId());

        // 3. Get existing submissions for this exam
        List<ExamSubmission> submissions = examSubmissionRepo.findByExamId(examId);

        List<LiveStudentMonitorDTO> monitorData = new ArrayList<>();

        // 4. Loop through ALL enrolled students
        for (ClassEnrollment enrollment : enrollments) {
            Student student = enrollment.getStudent();

            ExamSubmission studentSub = submissions.stream()
                    .filter(sub -> sub.getStudent().getId().equals(student.getId()))
                    .findFirst()
                    .orElse(null);

            if (studentSub != null) {
                String uiStatus = studentSub.getStatus().equals("SUBMITTED") ? "submitted" :
                        (studentSub.getStatus().equals("IN_PROGRESS") || studentSub.getStatus().equals("ACTIVE") ? "active" : "waiting");

                int flags = studentSub.getSuspiciousEventCount() != null ? studentSub.getSuspiciousEventCount() : 0;

                // 🟢 NEW: Fetch the specific log history for this student's submission
                List<ProctoringLog> rawLogs = proctoringLogRepo.findByExamSubmissionId(studentSub.getId());
                List<ProctoringLogDetailDTO> history = rawLogs.stream().map(log ->
                        ProctoringLogDetailDTO.builder()
                                .eventType(log.getEventType())
                                .durationSeconds(log.getDurationSeconds())
                                .recordedAt(log.getRecordedAt())
                                .build()
                ).collect(java.util.stream.Collectors.toList());

                monitorData.add(LiveStudentMonitorDTO.builder()
                        .id(student.getId())
                        .name(student.getFullName())
                        .status(uiStatus)
                        .flags(flags)
                        .totalAwaySeconds(studentSub.getTotalTimeAwaySeconds() != null ? studentSub.getTotalTimeAwaySeconds() : 0)
                        .flagged(flags > 0)
                        .proctoringHistory(history) // 🟢 Attach history to DTO
                        .build());
            } else {
                monitorData.add(LiveStudentMonitorDTO.builder()
                        .id(student.getId())
                        .name(student.getFullName())
                        .status("not started")
                        .flags(0)
                        .totalAwaySeconds(0)
                        .flagged(false)
                        .proctoringHistory(new ArrayList<>()) // Empty history
                        .build());
            }
        }

        return monitorData;
    }

    @Transactional
    @Override
    public void broadcastToExam(Integer examId, String teacherUsername, String message) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized: You do not own this exam.");
        }

        notificationService.dispatchExamBroadcast(examId, exam.getCourse().getTeacher().getFullName(), exam.getCourse().getName(), message);
    }

    @Transactional
    @Override
    public void warnStudent(Integer examId, Integer studentId, String teacherUsername, String message) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized: You do not own this exam.");
        }

        notificationService.dispatchStudentWarning(studentId, exam.getCourse().getTeacher().getFullName(), exam.getCourse().getName(), message);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PendingGradingDTO> getPendingPdfGradings(String teacherUsername) {
        // 1. Fetch all exams belonging to this teacher
        List<Exam> exams = examRepository.findByCourseTeacherUserAccountUsernameAndStatus(teacherUsername, "PUBLISHED");
        List<PendingGradingDTO> pendingList = new ArrayList<>();

        for (Exam exam : exams) {
            // 2. We only care about PDF exams
            if ("PDF".equalsIgnoreCase(exam.getExamType())) {
                List<ExamSubmission> submissions = examSubmissionRepo.findByExamId(exam.getId());

                for (ExamSubmission sub : submissions) {
                    // 3. Filter for submissions that are turned in but not yet fully graded
                    if ("SUBMITTED".equalsIgnoreCase(sub.getStatus()) && sub.getPdfSubmissionUrl() != null) {
                        pendingList.add(PendingGradingDTO.builder()
                                .id(sub.getId())
                                .examId(exam.getId())
                                .studentName(sub.getStudent().getFullName())
                                .examTitle(exam.getTitle())
                                .status("PENDING")
                                .pdfUrl(sub.getPdfSubmissionUrl())
                                .build());
                    }
                }
            }
        }
        return pendingList;
    }

    @Transactional
    @Override
    public void approveAndReleaseGrade(String teacherUsername, Integer submissionId, BigDecimal finalScore, BigDecimal calculatedScore, String feedback) {
        // 1. Fetch the submission
        ExamSubmission submission = examSubmissionRepo.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found."));

        // 2. Security Check: Ensure the teacher owns this class
        Course course = submission.getExam().getCourse();
        if (!course.getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized to grade this exam.");
        }

        // 3. 🟢 NEW: Calculate the Grade Letter automatically
        BigDecimal maxScore = submission.getExam().getMaxScore();
        String finalGrade = "N/A";

        if (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            // Calculate percentage
            BigDecimal percentage = finalScore.divide(maxScore, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // Apply Examsy grading logic
            double pct = percentage.doubleValue();
            if (pct < 40) finalGrade = "F";
            else if (pct < 55) finalGrade = "S";
            else if (pct < 65) finalGrade = "C";
            else if (pct < 75) finalGrade = "B";
            else finalGrade = "A";
        }

        // 4. Update the Submission record with ALL grading data
        // If there was no AI score (manually graded without AI), fallback to finalScore
        submission.setCalculatedScore(calculatedScore != null ? calculatedScore : finalScore);
        submission.setFinalScore(finalScore);
        submission.setAwardedGradeLetter(finalGrade);
        submission.setPdfFeedback(feedback);
        submission.setStatus("GRADED");

        examSubmissionRepo.save(submission);

        // 5. Trigger the Notification!
        notificationService.notifyStudentOfGradedExam(submission, course, course.getTeacher().getFullName(), finalScore);
    }

    @Transactional
    @Override
    public void triggerUpcomingExamReminders(String teacherUsername) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime within48Hours = now.plusHours(48);

        // 1. Get all published exams happening within the next 48 hours for this teacher
        List<Exam> upcomingExams = examRepository.findUpcomingExamsForReminders(teacherUsername, now, within48Hours);

        if (upcomingExams.isEmpty()) return; // Nothing to remind!

        for (Exam exam : upcomingExams) {
            // 2. Get all enrolled students for the class
            List<ClassEnrollment> enrollments = classEnrollmentRepo.findByCourseId(exam.getCourse().getId());

            // Fetch all submissions for this exam to easily check who finished
            List<ExamSubmission> submissions = examSubmissionRepo.findByExamId(exam.getId());

            for (ClassEnrollment enrollment : enrollments) {
                Student student = enrollment.getStudent();
                boolean hasFinished = false;

                // 3. Find if this specific student has a submission record
                java.util.Optional<ExamSubmission> studentSub = submissions.stream()
                        .filter(sub -> sub.getStudent().getId().equals(student.getId()))
                        .findFirst();

                if (studentSub.isPresent()) {
                    ExamSubmission submission = studentSub.get();
                    // 4. APPLY THE STRICT LOGIC: Do they have a final score, a grade letter, or are waiting for grading?
                    if (submission.getFinalScore() != null || submission.getAwardedGradeLetter() != null || "SUBMITTED".equalsIgnoreCase(submission.getStatus())) {
                        hasFinished = true;
                    }
                }

                // 5. If they haven't finished, send the reminder email!
                if (!hasFinished) {
                    String timeLabel = exam.getExamMode().contains("REAL") ? "Scheduled Start" : "Deadline";
                    String timeValue = exam.getExamMode().contains("REAL") ? exam.getScheduledStartTime().toString() : exam.getDeadlineTime().toString();

                    String message = "This is an automated reminder that you have an upcoming exam: '" + exam.getTitle() + "'.\n\n" +
                            "Course: " + exam.getCourse().getName() + "\n" +
                            timeLabel + ": " + timeValue + "\n\n" +
                            "Please ensure you are prepared and log in on time.";

                    // We reuse your existing notification logic here to handle user preferences and email formatting
                    notificationService.dispatchStudentWarning(
                            student.getId(),
                            exam.getCourse().getTeacher().getFullName(),
                            exam.getCourse().getName(),
                            message
                    );
                }
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ExamAnalyticsDTO getExamAnalytics(Integer examId, String teacherUsername) {
        // 1. Verify Ownership
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        if (!exam.getCourse().getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized access to exam analytics");
        }

        // 2. Fetch Data
        int totalEnrolled = classEnrollmentRepo.countByCourseId(exam.getCourse().getId());

        // Only fetch submissions that actually have a final score
        List<ExamSubmission> gradedSubmissions = examSubmissionRepo.findByExamId(examId).stream()
                .filter(sub -> sub.getFinalScore() != null)
                .collect(java.util.stream.Collectors.toList());

        // 3. Handle Empty State
        if (gradedSubmissions.isEmpty() || exam.getMaxScore() == null || exam.getMaxScore().compareTo(BigDecimal.ZERO) == 0) {
            return ExamAnalyticsDTO.builder()
                    .examId(examId)
                    .examTitle(exam.getTitle())
                    .averageScore("0.0")
                    .topScorerName("N/A")
                    .topScore("0.0")
                    .lowestScore("0.0")
                    .medianScore("0.0")
                    .totalStudents(totalEnrolled)
                    .participationRate("0.0")
                    .atRiskCount(0)
                    .passRate("0.0")
                    .gradeDistribution(Map.of("A (85+)", 0, "B (70-84)", 0, "C (55-69)", 0, "S (40-54)", 0, "F (<40)", 0))
                    .build();
        }

        // 4. Calculate Metrics
        double totalPct = 0;
        double highestPct = -1;
        double lowestPct = 101;
        String topStudent = "";
        int passedCount = 0;
        int atRiskCount = 0;

        List<Double> allPercentages = new java.util.ArrayList<>();
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("A (85+)", 0);
        distribution.put("B (70-84)", 0);
        distribution.put("C (55-69)", 0);
        distribution.put("S (40-54)", 0);
        distribution.put("F (<40)", 0);

        for (ExamSubmission sub : gradedSubmissions) {
            double pct = sub.getFinalScore().divide(exam.getMaxScore(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
            allPercentages.add(pct);
            totalPct += pct;

            if (pct > highestPct) {
                highestPct = pct;
                topStudent = sub.getStudent().getFullName();
            }
            if (pct < lowestPct) {
                lowestPct = pct;
            }

            if (pct >= 40) passedCount++;
            if (pct < 40) atRiskCount++;

            // Distribution Sorting
            if (pct >= 85) distribution.put("A (85+)", distribution.get("A (85+)") + 1);
            else if (pct >= 70) distribution.put("B (70-84)", distribution.get("B (70-84)") + 1);
            else if (pct >= 55) distribution.put("C (55-69)", distribution.get("C (55-69)") + 1);
            else if (pct >= 40) distribution.put("S (40-54)", distribution.get("S (40-54)") + 1);
            else distribution.put("F (<40)", distribution.get("F (<40)") + 1);
        }

        // 5. Final Math Operations
        double avg = totalPct / gradedSubmissions.size();
        double participation = ((double) gradedSubmissions.size() / totalEnrolled) * 100;
        double passRate = ((double) passedCount / gradedSubmissions.size()) * 100;

        java.util.Collections.sort(allPercentages);
        double median = 0;
        int size = allPercentages.size();
        if (size > 0) {
            if (size % 2 == 0) {
                median = (allPercentages.get(size / 2 - 1) + allPercentages.get(size / 2)) / 2.0;
            } else {
                median = allPercentages.get(size / 2);
            }
        }

        return ExamAnalyticsDTO.builder()
                .examId(examId)
                .examTitle(exam.getTitle())
                .averageScore(String.format("%.1f", avg))
                .topScorerName(topStudent)
                .topScore(String.format("%.1f", highestPct))
                .lowestScore(String.format("%.1f", lowestPct))
                .medianScore(String.format("%.1f", median))
                .totalStudents(totalEnrolled)
                .participationRate(String.format("%.1f", participation))
                .atRiskCount(atRiskCount)
                .passRate(String.format("%.0f", passRate))
                .gradeDistribution(distribution)
                .build();
    }
}