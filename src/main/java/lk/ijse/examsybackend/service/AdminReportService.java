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
                        .teacherComplaintCount(reportRepository.countByTargetCourseTeacher(r.getTargetCourse().getTeacher()))
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

    // Send a direct custom reply to the student
    @Transactional
    public void replyToStudent(Integer reportId, String messageBody) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        Student reporter = report.getReporterStudent();

        if (reporter.getNotifyIdentity() != null && reporter.getNotifyIdentity()) {
            sendInAppNotification(reporter.getUserAccount(), "Admin Update on Report #" + reportId, messageBody);
        }
        if (reporter.getNotifyEmail() != null && reporter.getNotifyEmail()) {
            sendEmail(reporter.getUserAccount().getUsername(), "Update on your Examsy Report", messageBody);
        }

        // Append note for admin records
        String existingNotes = report.getAdminNotes() == null ? "" : report.getAdminNotes() + "\n";
        report.setAdminNotes(existingNotes + "Admin Replied to Student: " + messageBody);
        reportRepository.save(report);
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

    // Send an official warning to the teacher AND acknowledge the student
    @Transactional
    public void warnTeacher(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Course course = report.getTargetCourse();
        Teacher teacher = course.getTeacher();
        Student reporter = report.getReporterStudent();

        // 1. Send Warning to Teacher
        if (teacher.getNotifySecurity() != null && teacher.getNotifySecurity()) {
            sendInAppNotification(teacher.getUserAccount(), "Official Warning",
                    "Your class '" + course.getName() + "' has received complaints regarding policy violations. Please review your content immediately.");
        }
        if (teacher.getNotifyEmail() != null && teacher.getNotifyEmail()) {
            sendEmail(teacher.getUserAccount().getUsername(), "Action Required: Examsy Official Warning",
                    "We have received reports about your class '" + course.getName() + "'. Please ensure your materials comply with our guidelines.");
        }

        // 2. Acknowledge Student (Crucial step!)
        if (reporter.getNotifyIdentity() != null && reporter.getNotifyIdentity()) {
            sendInAppNotification(reporter.getUserAccount(), "Report Reviewed",
                    "We have reviewed your report on '" + course.getName() + "' and issued an official warning to the instructor.");
        }

        // 3. Mark the report as resolved
        report.setStatus("RESOLVED");
        report.setAdminNotes("Official warning sent to teacher. Student acknowledged.");
        reportRepository.save(report);
    }

}