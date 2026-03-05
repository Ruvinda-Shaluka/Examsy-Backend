package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.AdminReportDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepo reportRepository;
    private final CourseRepo courseRepository;
    private final TeacherRepo teacherRepository;
    private final NotificationRepo notificationRepository;
    private final JavaMailSender mailSender; // Required for emails

    @Transactional(readOnly = true)
    public List<AdminReportDTO> getAllPendingReports() {
        return reportRepository.findAll().stream()
                .filter(r -> r.getStatus().equals("PENDING"))
                .map(r -> AdminReportDTO.builder()
                        .id(r.getId())
                        .category(r.getCategory())
                        .priorityLevel(r.getPriorityLevel())
                        .description(r.getDescription())
                        .status(r.getStatus())
                        .reportedAt(r.getReportedAt())
                        .classId(r.getTargetCourse().getId())
                        .className(r.getTargetCourse().getName())
                        .teacherName(r.getTargetCourse().getTeacher().getFullName())
                        .reporterName(r.getReporterStudent().getFullName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void terminateClass(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Course course = report.getTargetCourse();
        Teacher teacher = course.getTeacher();
        Student reporter = report.getReporterStudent();

        // 1. Delete the course (Make sure your DB handles cascading deletes for enrollments!)
        courseRepository.delete(course);

        // 2. Resolve Report
        report.setStatus("RESOLVED");
        report.setAdminNotes("Class terminated by Admin.");
        reportRepository.save(report);

        // 3. Notify Teacher (If preferences allow)
        if (teacher.getNotifySecurity() != null && teacher.getNotifySecurity()) {
            sendInAppNotification(teacher.getUserAccount(), "Class Terminated",
                    "Your class '" + course.getName() + "' was terminated due to policy violations.");
        }
        if (teacher.getNotifyEmail() != null && teacher.getNotifyEmail()) {
            sendEmail(teacher.getUserAccount().getUsername(), "Notice: Class Terminated",
                    "Your class '" + course.getName() + "' was removed by administration following a review.");
        }

        // 4. Acknowledge Student
        if (reporter.getNotifyIdentity() != null && reporter.getNotifyIdentity()) {
            sendInAppNotification(reporter.getUserAccount(), "Report Resolved",
                    "Action has been taken regarding your report on '" + course.getName() + "'.");
        }
    }

    @Transactional
    public void terminateTeacher(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Teacher teacher = report.getTargetCourse().getTeacher();

        // 1. Delete Teacher (This is destructive! Ensure cascades are set up)
        teacherRepository.delete(teacher);

        // 2. Resolve Report
        report.setStatus("RESOLVED");
        report.setAdminNotes("Teacher account terminated by Admin.");
        reportRepository.save(report);

        // 3. Email Teacher (Even if deleted, send a final notice)
        sendEmail(teacher.getUserAccount().getUsername(), "Account Termination Notice",
                "Your Examsy instructor account has been permanently terminated due to severe policy violations.");
    }

    @Transactional
    public void dismissReport(Integer reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        report.setStatus("DISMISSED");
        reportRepository.save(report);
    }

    // --- Helper Methods for Comms ---
    private void sendInAppNotification(UserAccount account, String title, String message) {
        Notification notification = Notification.builder()
                .userAccount(account)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail); // Assuming username is email. Adjust if needed.
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}