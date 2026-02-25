package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_submissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "student_id"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(length = 30)
    private String status = "NOT_STARTED";

    @Column(name = "proctoring_status", length = 30)
    private String proctoringStatus = "SECURE";

    @Column(name = "suspicious_event_count")
    private Integer suspiciousEventCount = 0;

    @Column(name = "total_time_away_seconds")
    private Integer totalTimeAwaySeconds = 0;

    @Column(name = "last_known_action", length = 100)
    private String lastKnownAction;

    @Column(name = "calculated_score", precision = 5, scale = 2)
    private BigDecimal calculatedScore;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "awarded_grade_letter", length = 2)
    private String awardedGradeLetter;

    @Column(name = "pdf_submission_url")
    private String pdfSubmissionUrl;
}
