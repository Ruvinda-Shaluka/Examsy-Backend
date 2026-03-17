package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResetPasswordDTO {
    private String email;
    private String code;
    private String newPassword;
}
