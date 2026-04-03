package lk.ijse.examsybackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CreateAnnouncementDTO {
    @NotBlank(message = "Announcement cannot be empty")
    private String content;
}
