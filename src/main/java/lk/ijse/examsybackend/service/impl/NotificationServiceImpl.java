package lk.ijse.examsybackend.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lk.ijse.examsybackend.dto.NotificationDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.ClassEnrollmentRepo;
import lk.ijse.examsybackend.repository.NotificationRepo;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.service.NotificationService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepo notificationRepository;
    private final StudentRepo studentRepository;
    private final TeacherRepo teacherRepository;
    private final ClassEnrollmentRepo classEnrollmentRepository;
    private final JavaMailSender mailSender;

    @Data
    @Builder
    private static class UserPrefs {
        private boolean pushEnabled;
        private boolean emailEnabled;
        private boolean identityEnabled;
    }

    private UserPrefs getUserPreferences(String username) {
        Optional<Student> studentOpt = studentRepository.findByUserAccountUsername(username);
        if (studentOpt.isPresent()) {
            Student s = studentOpt.get();
            return UserPrefs.builder()
                    .pushEnabled(s.getNotifyPush() != null && s.getNotifyPush())
                    .emailEnabled(s.getNotifyEmail() != null && s.getNotifyEmail())
                    .identityEnabled(s.getNotifyIdentity() != null && s.getNotifyIdentity())
                    .build();
        }

        Optional<Teacher> teacherOpt = teacherRepository.findByUserAccountUsername(username);
        if (teacherOpt.isPresent()) {
            Teacher t = teacherOpt.get();
            return UserPrefs.builder()
                    .pushEnabled(t.getNotifyPush() != null && t.getNotifyPush())
                    .emailEnabled(t.getNotifyEmail() != null && t.getNotifyEmail())
                    .identityEnabled(t.getNotifySecurity() != null && t.getNotifySecurity())
                    .build();
        }

        return UserPrefs.builder().pushEnabled(true).emailEnabled(true).identityEnabled(true).build();
    }

    private boolean isNotificationAllowed(Notification n, UserPrefs prefs) {
        String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";
        boolean isProctoringAlert = title.contains("proctoring") || title.contains("warning") || title.contains("security") || title.contains("identity");

        if (isProctoringAlert && !prefs.isIdentityEnabled()) {
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationDTO> getMyNotifications(String username) {
        UserPrefs prefs = getUserPreferences(username);

        if (!prefs.isPushEnabled()) {
            return Collections.emptyList();
        }

        return notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream()
                .filter(n -> isNotificationAllowed(n, prefs))
                .map(n -> NotificationDTO.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .courseId(n.getCourseId())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(String username) {
        UserPrefs prefs = getUserPreferences(username);
        if (!prefs.isPushEnabled()) return 0;

        return notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream()
                .filter(n -> !n.getIsRead())
                .filter(n -> isNotificationAllowed(n, prefs))
                .count();
    }

    @Transactional
    @Override
    public void markAsRead(Integer notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserAccount().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    @Override
    public void markAllAsRead(String username) {
        List<Notification> unread = notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream().filter(n -> !n.getIsRead()).collect(Collectors.toList());

        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional
    @Override
    public void dispatchAnnouncementNotifications(Integer courseId, String courseName, String teacherName, String content, boolean isUpdate) {
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByCourseId(courseId);
        List<Notification> notificationsToSave = new ArrayList<>();

        String actionWord = isUpdate ? "updated an announcement" : "posted a new announcement";
        String notificationTitle = (isUpdate ? "Update: " : "Announcement: ") + courseName;

        for (ClassEnrollment enrollment : enrollments) {
            UserAccount studentAccount = enrollment.getStudent().getUserAccount();
            UserPrefs prefs = getUserPreferences(studentAccount.getUsername());

            // 1. Check Push Notifications
            if (prefs.isPushEnabled()) {
                notificationsToSave.add(Notification.builder()
                        .userAccount(studentAccount)
                        .title(notificationTitle)
                        .message(teacherName + " " + actionWord + ": " + content)
                        .isRead(false)
                        .courseId(courseId)
                        .build());
            }

            // 2. Check Email Notifications
            if (prefs.isEmailEnabled() && studentAccount.getEmail() != null) {
                sendAnnouncementEmail(studentAccount.getEmail(), teacherName, courseName, content, isUpdate);
            }
        }

        if (!notificationsToSave.isEmpty()) {
            notificationRepository.saveAll(notificationsToSave);
        }
    }

    @Transactional
    @Override
    public void notifyTeacherOfJoinRequest(Course course, Student student) {
        Teacher teacher = course.getTeacher();
        UserAccount teacherAccount = teacher.getUserAccount();

        // Grab the teacher's notification settings using your existing helper
        UserPrefs prefs = getUserPreferences(teacherAccount.getUsername());

        String notificationTitle = "New Join Request: " + course.getName();
        String content = student.getFullName() + " has requested to join your class.";

        // 1. In-App Push Notification
        if (prefs.isPushEnabled()) {
            Notification pushNotif = Notification.builder()
                    .userAccount(teacherAccount)
                    .title(notificationTitle)
                    .message(content)
                    .isRead(false)
                    .courseId(course.getId())
                    .build();
            notificationRepository.save(pushNotif);
        }

        // 2. Email Notification
        if (prefs.isEmailEnabled() && teacherAccount.getEmail() != null) {
            try {
                jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
                org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(new jakarta.mail.internet.InternetAddress("noreply@examsy.com", "Examsy Notifications"));
                helper.setTo(teacherAccount.getEmail());
                helper.setSubject("Action Required: " + notificationTitle);

                String body = "Hello " + teacher.getFullName() + ",\n\n" +
                        content + "\n\n" +
                        "Please log in to your Examsy Teacher Dashboard and navigate to the 'People' tab of this class to approve or reject this request.";

                helper.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Failed to send join request email to teacher: " + e.getMessage());
            }
        }
    }

    @Transactional
    @Override
    public void notifyStudentOfJoinResult(Student student, Course course, boolean isApproved) {
        UserAccount studentAccount = student.getUserAccount();
        UserPrefs prefs = getUserPreferences(studentAccount.getUsername());

        String status = isApproved ? "Approved" : "Declined";
        String notificationTitle = "Class Join Request " + status;
        String content = isApproved
                ? "Your request to join '" + course.getName() + "' has been approved by the instructor. You can now access the classwork."
                : "Your request to join '" + course.getName() + "' was declined by the instructor.";

        // 1. Push Notification
        if (prefs.isPushEnabled()) {
            Notification pushNotif = Notification.builder()
                    .userAccount(studentAccount)
                    .title(notificationTitle)
                    .message(content)
                    .isRead(false)
                    // Only link to the course if approved, otherwise they can't access it anyway
                    .courseId(isApproved ? course.getId() : null)
                    .build();
            notificationRepository.save(pushNotif);
        }

        // 2. Email Notification
        if (prefs.isEmailEnabled() && studentAccount.getEmail() != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(new InternetAddress("noreply@examsy.com", "Examsy Notifications"));
                helper.setTo(studentAccount.getEmail());
                helper.setSubject(notificationTitle);

                String body = "Hello " + student.getFullName() + ",\n\n" +
                        content + "\n\n" +
                        (isApproved ? "Log in to Examsy to view your new class dashboard." : "If you believe this is an error, please contact your instructor.");

                helper.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Failed to send join result email to student: " + e.getMessage());
            }
        }
    }

    private void sendAnnouncementEmail(String toEmail, String teacherName, String courseName, String content, boolean isUpdate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // SPOOFING THE NAME: Shows up as "Shaluka Perera (Examsy)" but sends from noreply
            helper.setFrom(new InternetAddress("noreply@examsy.com", teacherName + " (Examsy)"));
            helper.setTo(toEmail);

            String subject = isUpdate ? "Updated Announcement in " + courseName : "New Announcement in " + courseName;
            helper.setSubject(subject);

            // Basic text formatting. You can upgrade this to helper.setText("<html>...</html>", true) later if you want HTML emails!
            String body = "Class: " + courseName + "\n" +
                    "Instructor: " + teacherName + "\n\n" +
                    "Message:\n" + content + "\n\n" +
                    "Log in to Examsy to view the full class stream.";

            helper.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send announcement email to " + toEmail + ": " + e.getMessage());
        }
    }
}