package lk.ijse.examsybackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamPublishDTO {

    @NotEmpty(message = "You must select at least one class.")
    private List<Integer> classIds; // Support assigning to multiple classes at once!

    @NotNull(message = "Exam title is required")
    private String title;

    private String examMode; // REAL_TIME or DEADLINE
    private String examType; // MCQ, SHORT, PDF

    private LocalDateTime scheduledStartTime;
    private LocalDateTime deadlineTime;

    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    private String pdfResourceUrl; // For PDF exams

    @Valid
    private List<QuestionDTO> questions; // For MCQ and Short Answer
}