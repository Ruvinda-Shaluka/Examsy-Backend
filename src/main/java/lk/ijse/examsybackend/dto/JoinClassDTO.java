package lk.ijse.examsybackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinClassDTO {

    @NotBlank(message = "Invite link is required.")
    private String inviteLink;

}