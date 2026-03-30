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
public class StudentAnalyticsDTO {
    private String gpa;
    private String bestScore;
    private String bestExam;
    private String lowestScore;
    private String lowestExam;
    private String rankText;
    private String rankSubText;
    private List<ExamChartDataDTO> chartData;
}