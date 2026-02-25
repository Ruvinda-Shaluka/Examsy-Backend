package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teachers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Teacher {
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

    @Column(name = "instructor_id", unique = true, length = 50)
    private String instructorId;

    @Column(length = 100)
    private String specialization;

    @Column(name = "office_location", length = 100)
    private String officeLocation;

    @Column(name = "professional_bio", columnDefinition = "TEXT")
    private String professionalBio;

    @Column(name = "notify_email")
    private Boolean notifyEmail = true;

    @Column(name = "notify_push")
    private Boolean notifyPush = false;

    @Column(name = "notify_security")
    private Boolean notifySecurity = true;
}
