package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "exam_mode", length = 20)
    private String examMode = "REAL_TIME";

    @Column(name = "exam_type", length = 20)
    private String examType = "MIXED";

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "deadline_time")
    private LocalDateTime deadlineTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "pdf_resource_url")
    private String pdfResourceUrl;

    @Column(name = "max_score", precision = 5, scale = 2)
    private java.math.BigDecimal maxScore;

    @Column(length = 20)
    private String status = "DRAFT";
}
