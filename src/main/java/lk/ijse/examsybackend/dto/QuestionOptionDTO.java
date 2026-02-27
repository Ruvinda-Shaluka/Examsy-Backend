package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOptionDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces the option belongs to a question
    @NotNull(message = "Question ID is required")
    private Integer questionId;

    // Prevents empty options
    @NotBlank(message = "Option text cannot be blank")
    private String optionText;

    // Ensures this boolean flag is explicitly passed
    @NotNull(message = "Must specify if this option is the correct answer")
    private Boolean isCorrect;
}