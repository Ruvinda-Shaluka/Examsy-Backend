package lk.ijse.examsybackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GoogleAuthDTO {
    private String token;
    private String role; // "student" or "teacher" - Needed if creating a new account
}