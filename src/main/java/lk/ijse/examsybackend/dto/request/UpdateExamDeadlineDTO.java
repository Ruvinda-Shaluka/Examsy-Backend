package lk.ijse.examsybackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateExamDeadlineDTO {
    private LocalDateTime scheduledStartTime;
    private LocalDateTime deadlineTime;
    private Integer durationMinutes;
}