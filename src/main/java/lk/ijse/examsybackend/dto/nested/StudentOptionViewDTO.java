package lk.ijse.examsybackend.dto.nested;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentOptionViewDTO {
    private Integer id;
    private String text;
}