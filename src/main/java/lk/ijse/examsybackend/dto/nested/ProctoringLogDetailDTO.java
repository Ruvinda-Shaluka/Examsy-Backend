package lk.ijse.examsybackend.dto.nested;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringLogDetailDTO {
    private String eventType;
    private Integer durationSeconds;
    private LocalDateTime recordedAt;
}