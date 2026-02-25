package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "auth_provider", length = 20)
    private String authProvider = "LOCAL";

    @Column(name = "preferred_theme", length = 10)
    private String preferredTheme = "dark";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
