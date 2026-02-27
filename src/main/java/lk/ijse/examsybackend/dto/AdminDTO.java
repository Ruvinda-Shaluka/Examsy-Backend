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
public class AdminDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces that an Admin must be linked to a User Account
    @NotNull(message = "User Account ID is required")
    private Integer userAccountId;

    // Prevents empty names
    @NotBlank(message = "Admin full name cannot be blank")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    private String profilePictureUrl;

    @Size(max = 50, message = "Role level cannot exceed 50 characters")
    private String roleLevel;
}