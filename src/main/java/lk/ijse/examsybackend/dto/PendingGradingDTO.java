package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingGradingDTO {
    private Integer id; // The ExamSubmission ID
    private Integer examId;
    private String studentName;
    private String examTitle;
    private String status;
    private String pdfUrl;
}