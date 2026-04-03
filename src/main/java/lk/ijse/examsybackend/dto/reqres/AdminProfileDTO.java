package lk.ijse.examsybackend.dto.reqres;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProfileDTO {
    private Integer id;
    private String fullName;
    private String profilePictureUrl;
    private String roleLevel;
}