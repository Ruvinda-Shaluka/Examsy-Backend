package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ClassAnnouncementDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces the announcement is posted in a specific class
    @NotNull(message = "Course ID is required")
    private Integer courseId;

    // Identifies who posted the announcement
    @NotNull(message = "Author User ID is required")
    private Integer authorUserId;

    // Prevents empty announcement posts
    @NotBlank(message = "Announcement content cannot be blank")
    private String content;

    private LocalDateTime createdAt;
}