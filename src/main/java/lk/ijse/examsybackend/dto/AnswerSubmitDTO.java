package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitDTO {

    @NotNull(message = "Question ID is absolutely required to map the answer.")
    private Integer questionId;

    private Integer selectedOptionId; // Optional: Used only for MCQ

    private String answerText; // Optional: Used only for Short Answer
}