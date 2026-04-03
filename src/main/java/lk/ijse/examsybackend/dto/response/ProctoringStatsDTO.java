package lk.ijse.examsybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProctoringStatsDTO {
    private Integer totalFlags;
    private Integer totalAwaySeconds;
}