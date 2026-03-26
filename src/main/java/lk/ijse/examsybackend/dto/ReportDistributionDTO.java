package lk.ijse.examsybackend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportDistributionDTO {
    private String type;
    private Long count;
}