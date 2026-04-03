package lk.ijse.examsybackend.dto.response;

import lk.ijse.examsybackend.dto.OngoingExamDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OngoingExamGroupDTO {
    private List<OngoingExamDTO> realTime;
    private List<OngoingExamDTO> deadline;
}