package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.response.NotificationDTO;
import lk.ijse.examsybackend.service.NotificationService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<APIResponse<List<NotificationDTO>>> getNotifications(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", notificationService.getMyNotifications(user.getUsername())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<APIResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", notificationService.getUnreadCount(user.getUsername())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<APIResponse<Void>> markAsRead(@PathVariable Integer id, @AuthenticationPrincipal UserDetails user) {
        notificationService.markAsRead(id, user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Marked as read", null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<APIResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllAsRead(user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "All marked as read", null));
    }
}