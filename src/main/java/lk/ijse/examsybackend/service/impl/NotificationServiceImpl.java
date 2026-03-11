package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.NotificationDTO;
import lk.ijse.examsybackend.entity.Notification;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.repository.NotificationRepo;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl {

    private final NotificationRepo notificationRepository;
    private final StudentRepo studentRepository;
    private final TeacherRepo teacherRepository;

    @Data
    @Builder
    private static class UserPrefs {
        private boolean pushEnabled;     // Controls all in-app notifications
        private boolean emailEnabled;    // Controls external emails
        private boolean identityEnabled; // Controls Proctoring/Security alerts
    }

    private UserPrefs getUserPreferences(String username) {
        // Check Student
        Optional<Student> studentOpt = studentRepository.findByUserAccountUsername(username);
        if (studentOpt.isPresent()) {
            Student s = studentOpt.get();
            return UserPrefs.builder()
                    .pushEnabled(s.getNotifyPush() != null && s.getNotifyPush())
                    .emailEnabled(s.getNotifyEmail() != null && s.getNotifyEmail())
                    .identityEnabled(s.getNotifyIdentity() != null && s.getNotifyIdentity())
                    .build();
        }

        // Check Teacher (Maps Teacher's notifySecurity to identityEnabled for unified logic)
        Optional<Teacher> teacherOpt = teacherRepository.findByUserAccountUsername(username);
        if (teacherOpt.isPresent()) {
            Teacher t = teacherOpt.get();
            return UserPrefs.builder()
                    .pushEnabled(t.getNotifyPush() != null && t.getNotifyPush())
                    .emailEnabled(t.getNotifyEmail() != null && t.getNotifyEmail())
                    .identityEnabled(t.getNotifySecurity() != null && t.getNotifySecurity())
                    .build();
        }

        // Failsafe default
        return UserPrefs.builder().pushEnabled(true).emailEnabled(true).identityEnabled(true).build();
    }

    private boolean isNotificationAllowed(Notification n, UserPrefs prefs) {
        String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";

        // Identify if this is a Proctoring/Security alert
        boolean isProctoringAlert = title.contains("proctoring") || title.contains("warning") || title.contains("security") || title.contains("identity");

        // If it is a proctoring alert, but the user disabled identity notifications, BLOCK IT.
        if (isProctoringAlert && !prefs.isIdentityEnabled()) {
            return false;
        }

        return true; // Pass all other notifications
    }

    // --- STANDARD READ OPERATIONS ---

    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(String username) {
        UserPrefs prefs = getUserPreferences(username);

        // GATEKEEPER: If push is completely off, return empty list instantly!
        if (!prefs.isPushEnabled()) {
            return Collections.emptyList();
        }

        return notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream()
                .filter(n -> isNotificationAllowed(n, prefs)) // 🟢 Apply Proctoring filter
                .map(n -> NotificationDTO.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        UserPrefs prefs = getUserPreferences(username);

        // GATEKEEPER: If push is completely off, unread count is 0!
        if (!prefs.isPushEnabled()) {
            return 0;
        }

        return notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream()
                .filter(n -> !n.getIsRead())
                .filter(n -> isNotificationAllowed(n, prefs)) // 🟢 Apply Proctoring filter
                .count();
    }

    @Transactional
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
    public void markAllAsRead(String username) {
        List<Notification> unread = notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream().filter(n -> !n.getIsRead()).collect(Collectors.toList());

        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }
}