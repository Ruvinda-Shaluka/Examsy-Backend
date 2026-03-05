package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserAccountUsernameOrderByCreatedAtDesc(String username);
}