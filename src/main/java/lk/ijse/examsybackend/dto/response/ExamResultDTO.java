package lk.ijse.examsybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultDTO {
    private BigDecimal score;
    private BigDecimal maxScore;
    private String status;
    private String message;
    private BigDecimal percentage;
    private String grade;
}