package lk.ijse.examsybackend.dto.nested;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionDTO {
    private String questionText;
    private BigDecimal points;
    private List<String> options; // E.g., ["Option 1", "Option 2"]
    private String modelAnswer;
    private Integer correctOptionIndex; // Index of the correct answer (0, 1, 2...)
}