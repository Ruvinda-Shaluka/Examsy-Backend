package lk.ijse.examsybackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentExamViewDTO {
    private Integer id;
    private String title;
    private String examType; // MCQ, SHORT, PDF
    private Integer durationMinutes;
    private String pdfResourceUrl;
    private List<StudentQuestionViewDTO> questions;
}
