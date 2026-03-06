package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.NotificationDTO;
import lk.ijse.examsybackend.entity.Notification;
import lk.ijse.examsybackend.repository.NotificationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepo notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(String username) {
        return notificationRepository.findByUserAccountUsernameOrderByCreatedAtDesc(username)
                .stream().map(n -> NotificationDTO.builder()
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
        return notificationRepository.countByUserAccountUsernameAndIsReadFalse(username);
    }

    @Transactional
    public void markAsRead(Integer notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Security check: Ensure they own this notification
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