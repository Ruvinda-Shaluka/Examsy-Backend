package lk.ijse.examsybackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProfileUpdateDTO {
    @NotBlank(message = "Full name is strictly required.")
    private String fullName;

    private String profilePictureUrl; // Optional, only sent if updated
}