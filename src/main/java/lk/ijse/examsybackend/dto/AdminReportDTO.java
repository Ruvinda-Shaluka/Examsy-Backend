package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportDTO {
    private Integer id;
    private String category;
    private String priorityLevel;
    private String description;
    private String status;
    private LocalDateTime reportedAt;

    // Extracted target data
    private Integer classId;
    private String className;
    private String teacherName;
    private String reporterName;
    private Long teacherComplaintCount;
}