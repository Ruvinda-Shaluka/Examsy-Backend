package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.NotificationDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationService {
    @Transactional(readOnly = true)
    List<NotificationDTO> getMyNotifications(String username);

    @Transactional(readOnly = true)
    long getUnreadCount(String username);

    @Transactional
    void markAsRead(Integer notificationId, String username);

    @Transactional
    void markAllAsRead(String username);
}
