package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProctoringLogDTO {
    @NotBlank(message = "Event type is required")
    private String eventType; // "TAB_SWITCHED", "WINDOW_LOST_FOCUS", or "SPLIT_SCREEN"

    @NotNull(message = "Duration is required")
    private Integer durationSeconds;
}