package lk.ijse.examsybackend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExamResultDTO {
    private BigDecimal score;
    private BigDecimal maxScore;
    private String status;
    private String message;
}
