package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admins")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_account_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "role_level", length = 50)
    private String roleLevel = "SUPER_ADMIN";
}
