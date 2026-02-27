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
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces the exam belongs to a course
    @NotNull(message = "Course ID is required")
    private Integer courseId;

    // Prevents empty exam titles
    @NotBlank(message = "Exam title cannot be blank")
    @Size(max = 255, message = "Exam title cannot exceed 255 characters")
    private String title;

    private String instructions;

    @Size(max = 20, message = "Exam mode cannot exceed 20 characters")
    private String examMode;

    @Size(max = 20, message = "Exam type cannot exceed 20 characters")
    private String examType;

    private LocalDateTime scheduledStartTime;
    private LocalDateTime deadlineTime;

    // Ensures an exam has a logical duration
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero minutes")
    private Integer durationMinutes;

    private String pdfResourceUrl;

    @PositiveOrZero(message = "Max score cannot be negative")
    private BigDecimal maxScore;

    @Size(max = 20, message = "Status cannot exceed 20 characters")
    private String status;
}