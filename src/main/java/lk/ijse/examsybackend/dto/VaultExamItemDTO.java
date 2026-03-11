package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultExamItemDTO {
    private Integer id;
    private String title;
    private String examType;
    private Integer durationMinutes;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime deadlineTime;
    private String status;
    private String studentStatus; // "Not Attempted", "In Progress", "Completed"
}