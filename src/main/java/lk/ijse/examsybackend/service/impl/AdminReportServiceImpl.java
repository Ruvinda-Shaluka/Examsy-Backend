package lk.ijse.examsybackend.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lk.ijse.examsybackend.dto.AdminReportDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportRepo reportRepository;
    private final CourseRepo courseRepository;
    private final TeacherRepo teacherRepository;
    private final NotificationRepo notificationRepository;
    private final JavaMailSender mailSender;

    @Transactional(readOnly = true)
    @Override
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
    @Override
    public void terminateClass(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Course course = report.getTargetCourse();
        Teacher teacher = course.getTeacher();
        Student reporter = report.getReporterStudent();

        // 1. Delete the course
        courseRepository.delete(course);

        // 2. Resolve Report
        report.setStatus("RESOLVED");
        report.setAdminNotes("Class terminated by Admin.");
        reportRepository.save(report);

        // 3. Notify Teacher (Passing null for courseId since it's deleted)
        if (teacher.getNotifySecurity() != null && teacher.getNotifySecurity()) {
            sendInAppNotification(teacher.getUserAccount(), "Class Terminated",
                    "Your class '" + course.getName() + "' was terminated due to policy violations.", null);
        }
        if (teacher.getNotifyEmail() != null && teacher.getNotifyEmail()) {
            sendEmail(teacher.getUserAccount().getEmail(), "Notice: Class Terminated",
                    "Your class '" + course.getName() + "' was removed by administration following a review.");
        }

        // 4. Acknowledge Student (Passing null for courseId)
        if (reporter.getNotifyIdentity() != null && reporter.getNotifyIdentity()) {
            sendInAppNotification(reporter.getUserAccount(), "Report Resolved",
                    "Action has been taken regarding your report on '" + course.getName() + "'.", null);
        }
    }

    @Transactional
    @Override
    public void replyToStudent(Integer reportId, String messageBody) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        Student reporter = report.getReporterStudent();
        Integer courseId = report.getTargetCourse().getId();

        // Passing courseId so the student can click the notification and return to the class
        if (reporter.getNotifyIdentity() != null && reporter.getNotifyIdentity()) {
            sendInAppNotification(reporter.getUserAccount(), "Admin Update on Report #" + reportId, messageBody, courseId);
        }
        if (reporter.getNotifyEmail() != null && reporter.getNotifyEmail()) {
            sendEmail(reporter.getUserAccount().getEmail(), "Update on your Examsy Report", messageBody);
        }

        String existingNotes = report.getAdminNotes() == null ? "" : report.getAdminNotes() + "\n";
        report.setAdminNotes(existingNotes + "Admin Replied to Student: " + messageBody);
        reportRepository.save(report);
    }

    @Transactional
    @Override
    public void terminateTeacher(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Teacher teacher = report.getTargetCourse().getTeacher();

        // 1. Delete Teacher
        teacherRepository.delete(teacher);

        // 2. Resolve Report
        report.setStatus("RESOLVED");
        report.setAdminNotes("Teacher account terminated by Admin.");
        reportRepository.save(report);

        // 3. Email Teacher
        sendEmail(teacher.getUserAccount().getEmail(), "Account Termination Notice",
                "Your Examsy instructor account has been permanently terminated due to severe policy violations.");
    }

    @Transactional
    @Override
    public void dismissReport(Integer reportId) {
        Report report = reportRepository.findById(reportId).orElseThrow();
        report.setStatus("DISMISSED");
        reportRepository.save(report);
    }

    @Transactional
    @Override
    public void warnTeacher(Integer reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        Course course = report.getTargetCourse();
        Teacher teacher = course.getTeacher();
        Student reporter = report.getReporterStudent();

        // 1. Send Warning to Teacher (Pass courseId so they can click it!)
        if (teacher.getNotifySecurity() != null && teacher.getNotifySecurity()) {
            sendInAppNotification(teacher.getUserAccount(), "Official Warning",
                    "Your class '" + course.getName() + "' has received complaints regarding policy violations. Please review your content immediately.", course.getId());
        }
        if (teacher.getNotifyEmail() != null && teacher.getNotifyEmail()) {
            sendEmail(teacher.getUserAccount().getEmail(), "Action Required: Examsy Official Warning",
                    "We have received reports about your class '" + course.getName() + "'. Please ensure your materials comply with our guidelines.");
        }

        // 2. Acknowledge Student
        if (reporter.getNotifyIdentity() != null && reporter.getNotifyIdentity()) {
            sendInAppNotification(reporter.getUserAccount(), "Report Reviewed",
                    "We have reviewed your report on '" + course.getName() + "' and issued an official warning to the instructor.", course.getId());
        }

        // 3. Mark the report as resolved
        report.setStatus("RESOLVED");
        report.setAdminNotes("Official warning sent to teacher. Student acknowledged.");
        reportRepository.save(report);
    }


    // Added courseId parameter to enable click-to-navigate in the frontend
    private void sendInAppNotification(UserAccount account, String title, String message, Integer courseId) {
        Notification notification = Notification.builder()
                .userAccount(account)
                .title(title)
                .message(message)
                .isRead(false)
                .courseId(courseId)
                .build();
        notificationRepository.save(notification);
    }

    // Upgraded to MimeMessage to display "Examsy Administration" as the sender name
    private void sendEmail(String toEmail, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress("noreply@examsy.com", "Examsy Administration"));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send admin email to " + toEmail + ": " + e.getMessage());
        }
    }
}