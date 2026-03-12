package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.NotificationDTO;
import lk.ijse.examsybackend.entity.ClassEnrollment;
import lk.ijse.examsybackend.entity.Notification;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.entity.UserAccount;
import lk.ijse.examsybackend.repository.ClassEnrollmentRepo;
import lk.ijse.examsybackend.repository.NotificationRepo;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.service.NotificationService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
                        // 🟢 MAP THE COURSE ID FOR REACT HERE
                        .courseId(n.getCourseId())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(String username) {
        UserPrefs prefs = getUserPreferences(username);

        if (!prefs.isPushEnabled()) {
            return 0;
        }

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
    public void dispatchAnnouncementNotifications(Integer courseId, String classCode, String authorName, String content) {
        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByCourseId(courseId);
        List<Notification> notificationsToSave = new ArrayList<>();

        for (ClassEnrollment enrollment : enrollments) {
            UserAccount studentAccount = enrollment.getStudent().getUserAccount();
            UserPrefs prefs = getUserPreferences(studentAccount.getUsername());

            if (prefs.isPushEnabled()) {
                notificationsToSave.add(Notification.builder()
                        .userAccount(studentAccount)
                        .title("Announcement: " + classCode)
                        .message(authorName + " posted: " + content)
                        .isRead(false)
                        // 🟢 SAVE THE COURSE ID TO THE DATABASE
                        .courseId(courseId)
                        .build());
            }
        }

        if (!notificationsToSave.isEmpty()) {
            notificationRepository.saveAll(notificationsToSave);
        }
    }
}