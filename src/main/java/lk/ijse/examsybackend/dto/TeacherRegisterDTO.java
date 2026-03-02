package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TeacherRegisterDTO {
    // --- Step 1 Fields ---
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Work email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // --- Step 2 Fields ---
    @NotBlank(message = "Instructor ID is required")
    private String instructorId;

    @NotBlank(message = "Specialization is required")
    private String specialization;
}