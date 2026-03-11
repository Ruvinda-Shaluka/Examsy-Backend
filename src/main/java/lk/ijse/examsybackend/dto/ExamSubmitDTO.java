package lk.ijse.examsybackend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamSubmitDTO {
    private String pdfSubmissionUrl; // If it's a PDF exam
    private List<AnswerSubmitDTO> answers;
}
