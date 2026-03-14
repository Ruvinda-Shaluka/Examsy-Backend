package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ExamSummaryDTO {
    private Integer id;
    private String title;
    private String examType; // MCQ, SHORT, PDF
    private String status;
    private LocalDateTime deadlineTime;
    private Integer durationMinutes;
    private BigDecimal maxScore;
}