package lk.ijse.examsybackend.dto.response;

import lk.ijse.examsybackend.dto.AnnouncementDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ClassStreamDTO {
    private String classCode;
    private String title;
    private String section;
    private String themeColorHex;
    private String bannerImageUrl;
    private List<AnnouncementDTO> announcements;
}