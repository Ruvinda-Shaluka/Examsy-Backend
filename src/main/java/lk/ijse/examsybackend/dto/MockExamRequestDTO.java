package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MockExamRequestDTO {
    private String subject;
    private String topic;
    private String difficulty;
    private int count;
}