package lk.ijse.examsybackend.dto;

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
public class ExamSubmissionDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Ensures submission connects to an exam
    @NotNull(message = "Exam ID is required")
    private Integer examId;

    // Ensures submission connects to a student
    @NotNull(message = "Student ID is required")
    private Integer studentId;

    private LocalDateTime actualStartTime;
    private LocalDateTime submittedAt;

    @Size(max = 30, message = "Status cannot exceed 30 characters")
    private String status;

    @Size(max = 30, message = "Proctoring status cannot exceed 30 characters")
    private String proctoringStatus;

    // Ensures proctoring analytics don't break with negative numbers
    @PositiveOrZero(message = "Suspicious event count cannot be negative")
    private Integer suspiciousEventCount;

    @PositiveOrZero(message = "Total time away cannot be negative")
    private Integer totalTimeAwaySeconds;

    @Size(max = 100, message = "Last known action string is too long")
    private String lastKnownAction;

    @PositiveOrZero(message = "Calculated score cannot be negative")
    private BigDecimal calculatedScore;

    @PositiveOrZero(message = "Final score cannot be negative")
    private BigDecimal finalScore;

    @Size(max = 2, message = "Awarded grade letter cannot exceed 2 characters")
    private String awardedGradeLetter;

    private String pdfSubmissionUrl;
}