package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
class AnnouncementDTO {
    private Integer id;
    private String authorName;
    private String content;
    private String formattedDate;
}
