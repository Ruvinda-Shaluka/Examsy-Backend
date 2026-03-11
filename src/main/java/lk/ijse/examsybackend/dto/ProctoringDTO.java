package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProctoringDTO {
    private Integer examId;
    private String eventType;
    private Integer durationSeconds;
}