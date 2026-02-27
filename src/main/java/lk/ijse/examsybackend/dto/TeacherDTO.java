package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces that a Teacher must be linked to a User Account
    @NotNull(message = "User Account ID is required.")
    private Integer userAccountId;

    // Prevents empty names
    @NotBlank(message = "Teacher full name cannot be blank.")
    @Size(max = 100, message = "Full name cannot exceed 100 characters.")
    private String fullName;

    private String profilePictureUrl;

    // Ensures the instructor ID string isn't too long
    @Size(max = 50, message = "Instructor ID cannot exceed 50 characters.")
    private String instructorId;

    @Size(max = 100, message = "Specialization cannot exceed 100 characters.")
    private String specialization;

    @Size(max = 100, message = "Office location cannot exceed 100 characters.")
    private String officeLocation;

    @Size(max = 300, message = "Professional bio cannot exceed 300 characters.")
    private String professionalBio;
    private Boolean notifyEmail;
    private Boolean notifyPush;
    private Boolean notifySecurity;
}