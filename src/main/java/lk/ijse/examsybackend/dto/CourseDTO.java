package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CourseDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces the course has a designated teacher
    @NotNull(message = "Teacher ID is required")
    private Integer teacherId;

    // Prevents empty course names
    @NotBlank(message = "Course name cannot be blank")
    @Size(max = 100, message = "Course name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Section name cannot exceed 50 characters")
    private String sectionName;

    @Size(max = 50, message = "Academic term cannot exceed 50 characters")
    private String academicTerm;

    // Prevents empty class codes for enrollments
    @NotBlank(message = "Class code is required")
    @Size(max = 20, message = "Class code cannot exceed 20 characters")
    private String classCode;

    private String bannerImageUrl;

    // Ensures the hex color code is formatted properly for frontend rendering
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Theme color must be a valid Hex code")
    @Size(max = 7, message = "Theme color cannot exceed 7 characters")
    private String themeColorHex;

    private Boolean isArchived;
    private LocalDateTime createdAt;
}