package lk.ijse.examsybackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mock_exams")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MockExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;

    @OneToMany(mappedBy = "mockExam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockQuestion> questions;
}