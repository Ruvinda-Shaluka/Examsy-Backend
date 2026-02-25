package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "classes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "section_name", length = 50)
    private String sectionName;

    @Column(name = "academic_term", length = 50)
    private String academicTerm;

    @Column(name = "class_code", nullable = false, unique = true, length = 20)
    private String classCode;

    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Column(name = "theme_color_hex", length = 7)
    private String themeColorHex = "#4F46E5";

    @Column(name = "is_archived")
    private Boolean isArchived = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
