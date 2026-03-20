package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LiveStudentMonitorDTO {
    private Integer id; // Student ID
    private String name;
    private String status; // "active" or "submitted"
    private Integer flags;
    private Integer totalAwaySeconds;
    private Boolean flagged; // True if flags > 0
    private List<ProctoringLogDetailDTO> proctoringHistory;
}