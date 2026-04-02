package lk.ijse.examsybackend.dto.response;

import lk.ijse.examsybackend.dto.VaultExamItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultExamsResponseDTO {
    private List<VaultExamItemDTO> upcomingExams;
    private List<VaultExamItemDTO> availableExams;
}