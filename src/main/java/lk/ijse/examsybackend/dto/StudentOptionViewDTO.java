package lk.ijse.examsybackend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class StudentOptionViewDTO {
    private Integer id;
    private String text;
}
