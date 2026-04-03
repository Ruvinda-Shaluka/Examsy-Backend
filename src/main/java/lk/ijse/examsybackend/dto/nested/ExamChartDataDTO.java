package lk.ijse.examsybackend.dto.nested;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ExamChartDataDTO {
    private String exam;
    private Double score;
}