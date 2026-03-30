package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ExamAnalyticsDTO {
    private Integer examId;
    private String examTitle;
    private String averageScore;
    private String topScorerName;
    private String topScore;
    private String lowestScore;
    private String medianScore;
    private Integer totalStudents;
    private String participationRate;
    private Integer atRiskCount;
    private String passRate;
    private Map<String, Integer> gradeDistribution;
}