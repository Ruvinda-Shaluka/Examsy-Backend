package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Ensures we know who filed the report
    @NotNull(message = "Reporter Student ID is required")
    private Integer reporterStudentId;

    // Ensures we know which class is being reported
    @NotNull(message = "Target Course ID is required")
    private Integer targetCourseId;

    // Ensures the category is defined
    @NotBlank(message = "Report category is required")
    @Size(max = 50, message = "Category name cannot exceed 50 characters")
    private String category;

    @NotBlank(message = "Priority level is required")
    @Size(max = 20, message = "Priority level cannot exceed 20 characters")
    private String priorityLevel;

    // Ensures the student actually typed a reason for the report
    @NotBlank(message = "Report description cannot be empty")
    private String description;

    @Size(max = 30, message = "Status cannot exceed 30 characters")
    private String status;

    @Size(max = 500, message = "Admin notes cannot exceed 500 characters")
    private String adminNotes;

    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private Integer resolvedByAdminId;
}