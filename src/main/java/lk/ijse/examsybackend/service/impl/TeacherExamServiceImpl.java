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
import java.util.List;

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

        // 1. Calculate Total Max Score
        BigDecimal maxScore = dto.getMaxScore() != null ? dto.getMaxScore() : BigDecimal.ZERO;

        if (maxScore.compareTo(BigDecimal.ZERO) == 0 && dto.getQuestions() != null) {
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
}