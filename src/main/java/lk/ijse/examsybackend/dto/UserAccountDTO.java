package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class UserAccountDTO {

    // Ensures the ID is a valid positive number
    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Prevents empty, null, or blank usernames
    @NotBlank(message = "Username cannot be blank")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    private String username;

    // Ensures the email is properly formatted (e.g., user@email.com)
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    // We don't use @NotBlank here because passwords might not be updated on every request
    private String passwordHash;

    // Restricts the length of the auth provider string
    @Size(max = 20, message = "Auth provider name is too long")
    private String authProvider;

    // Restricts the theme string size
    @Size(max = 10, message = "Theme preference name is too long")
    private String preferredTheme;

    private Boolean isActive;
    private LocalDateTime createdAt;
}