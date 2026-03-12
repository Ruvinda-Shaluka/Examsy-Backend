package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherClassCardDTO {
    private Integer id;
    private String title;
    private String section;
    private String themeColorHex;
    private String bannerImageUrl;
}