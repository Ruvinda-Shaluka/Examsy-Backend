package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
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

    @Column(name = "student_identification_number", unique = true, length = 50)
    private String studentIdentificationNumber;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 20)
    private String grade;

    @Column(length = 100)
    private String major;

    @Column(name = "academic_bio", columnDefinition = "TEXT")
    private String academicBio;

    @Column(name = "cumulative_gpa", precision = 3, scale = 2)
    private java.math.BigDecimal cumulativeGpa;

    @Column(name = "notify_email")
    private Boolean notifyEmail = true;

    @Column(name = "notify_push")
    private Boolean notifyPush = false;

    @Column(name = "notify_identity")
    private Boolean notifyIdentity = true;
}
