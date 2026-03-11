package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamViewDTO {
    private Integer id;
    private String title;
    private String examType; // MCQ, SHORT, PDF
    private Integer durationMinutes;
    private String pdfResourceUrl;
    private List<StudentQuestionViewDTO> questions;
}