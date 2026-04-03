package lk.ijse.examsybackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateAppearanceDTO {
    private String themeColorHex;
    private String bannerImageUrl;
}