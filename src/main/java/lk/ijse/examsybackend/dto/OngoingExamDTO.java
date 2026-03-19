package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OngoingExamDTO {
    private Integer id;
    private String title;
    private String className;
    private String examMode; // "REAL_TIME" or "DEADLINE"
    // Stats
    private Integer activeStudents;
    private Integer submissions;
    private Integer totalStudents;
    // Time Strings
    private String remainingTime;
    private String deadline;
}