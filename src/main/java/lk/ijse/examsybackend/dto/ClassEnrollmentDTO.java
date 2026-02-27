package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEnrollmentDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces the relationship to the course
    @NotNull(message = "Course ID is required")
    private Integer courseId;

    // Enforces the relationship to the student
    @NotNull(message = "Student ID is required")
    private Integer studentId;

    private LocalDateTime enrolledAt;
}