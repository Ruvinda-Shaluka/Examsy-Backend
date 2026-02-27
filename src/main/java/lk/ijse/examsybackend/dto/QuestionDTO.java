package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces the question belongs to an exam
    @NotNull(message = "Exam ID is required")
    private Integer examId;

    // Prevents empty questions
    @NotBlank(message = "Question text cannot be blank")
    private String questionText;

    // Ensures a type is provided (e.g., MCQ, SHORT_ANSWER)
    @NotBlank(message = "Question type is required")
    @Size(max = 50, message = "Question type cannot exceed 50 characters")
    private String questionType;

    // Prevents negative point values on questions
    @PositiveOrZero(message = "Points cannot be negative")
    private BigDecimal points;

    @PositiveOrZero(message = "Order index cannot be negative")
    private Integer orderIndex;
}