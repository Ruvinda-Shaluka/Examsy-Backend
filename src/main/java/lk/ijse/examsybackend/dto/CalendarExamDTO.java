package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CalendarExamDTO {
    private Integer id;
    private Integer classId;
    private String title;
    private String courseName;
    private String themeColorHex;
    private LocalDateTime examDate;
    private String examMode;
}