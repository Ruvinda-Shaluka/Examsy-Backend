package lk.ijse.examsybackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringDTO {

    @NotNull(message = "Exam ID is required")
    private Integer examId;

    @NotBlank(message = "Event type cannot be blank")
    private String eventType;

    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration cannot be negative")
    private Integer durationSeconds;
}