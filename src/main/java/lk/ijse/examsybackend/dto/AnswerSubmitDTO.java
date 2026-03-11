package lk.ijse.examsybackend.dto;

import lombok.Data;

@Data
public class AnswerSubmitDTO {
    private Integer questionId;
    private Integer selectedOptionId; // For MCQ
    private String answerText; // For Short Answer
}
