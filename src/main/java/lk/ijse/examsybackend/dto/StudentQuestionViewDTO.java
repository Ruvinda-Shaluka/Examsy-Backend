package lk.ijse.examsybackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentQuestionViewDTO {
    private Integer id;
    private String text;
    private String type;
    private List<StudentOptionViewDTO> options;
}