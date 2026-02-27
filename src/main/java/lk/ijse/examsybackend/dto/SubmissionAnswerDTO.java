package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionAnswerDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Ensures this answer is linked to an actual submission
    @NotNull(message = "Submission ID is required")
    private Integer submissionId;

    // Ensures this answer is linked to a specific question
    @NotNull(message = "Question ID is required")
    private Integer questionId;

    private String answerText;

    private Integer selectedOptionId;

    // Ensures teachers can't accidentally award negative points
    @PositiveOrZero(message = "Score awarded cannot be negative")
    private BigDecimal scoreAwarded;
}