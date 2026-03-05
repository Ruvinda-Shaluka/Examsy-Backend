package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClassCardDTO {
    private Integer id;           // Maps to course.id
    private String title;         // Maps to course.name
    private String section;       // Maps to course.sectionName
    private String bannerColor;   // Maps to course.themeColorHex
    private String teacher;       // Maps to course.teacher.fullName
}