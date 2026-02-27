package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDTO {

    @Positive(message = "ID must be a valid positive integer")
    private Integer id;

    // Enforces that a Student must be linked to a User Account
    @NotNull(message = "User Account ID is required")
    private Integer userAccountId;

    // Prevents empty names
    @NotBlank(message = "Student full name cannot be blank")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    private String profilePictureUrl;

    @Size(max = 50, message = "Student ID number cannot exceed 50 characters")
    private String studentIdentificationNumber;

    // Ensures the date provided is strictly in the past (no future birthdays)
    @Past(message = "Date of birth must be a past date")
    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Gender cannot exceed 20 characters")
    private String gender;

    @Size(max = 20, message = "Grade cannot exceed 20 characters")
    private String grade;

    @Size(max = 100, message = "Major cannot exceed 100 characters")
    private String major;

    @Size(max = 300, message = "Academic bio cannot exceed 300 characters")
    private String academicBio;

    // Ensures teachers can't accidentally award negative points
    @PositiveOrZero(message = "Cumulative GPA awarded cannot be negative")
    private BigDecimal cumulativeGpa;
    private Boolean notifyEmail;
    private Boolean notifyPush;
    private Boolean notifyIdentity;
}