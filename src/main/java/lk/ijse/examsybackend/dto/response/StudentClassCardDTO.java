package lk.ijse.examsybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentClassCardDTO {
    private Integer id;
    private String title;
    private String section;
    private String themeColorHex;
    private String bannerImageUrl;
    private String teacher;
}