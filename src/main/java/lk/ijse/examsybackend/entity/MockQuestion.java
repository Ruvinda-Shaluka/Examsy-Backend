package lk.ijse.examsybackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mock_questions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MockQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_exam_id", nullable = false)
    @JsonIgnore
    private MockExam mockExam;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(nullable = false)
    private String optionA;

    @Column(nullable = false)
    private String optionB;

    @Column(nullable = false)
    private String optionC;

    @Column(nullable = false)
    private String optionD;

    @Column(nullable = false)
    private Integer correctOptionIndex; // 0=A, 1=B, 2=C, 3=D

    @Column(columnDefinition = "TEXT")
    private String explanation;
}