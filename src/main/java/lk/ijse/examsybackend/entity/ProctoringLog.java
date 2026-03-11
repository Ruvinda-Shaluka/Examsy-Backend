package lk.ijse.examsybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proctoring_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProctoringLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Links directly to the student's specific exam attempt
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private ExamSubmission examSubmission;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // e.g., "TAB_SWITCHED", "WINDOW_MINIMIZED"

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}