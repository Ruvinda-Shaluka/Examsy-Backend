package lk.ijse.examsybackend.dto.request;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.AnswerSubmitDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSubmitDTO {

    private String pdfSubmissionUrl; // Optional: Only populated if it's a PDF exam

    @Valid // Cascades validation down to the individual answers!
    private List<AnswerSubmitDTO> answers;
}