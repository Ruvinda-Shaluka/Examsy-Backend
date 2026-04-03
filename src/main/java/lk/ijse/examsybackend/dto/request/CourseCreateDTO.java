package lk.ijse.examsybackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCreateDTO {

    @NotBlank(message = "Class name is strictly required.")
    @Size(max = 100, message = "Class name cannot exceed 100 characters.")
    private String name;

    @Size(max = 50, message = "Section name cannot exceed 50 characters.")
    private String sectionName;

    @Size(max = 50, message = "Academic term cannot exceed 50 characters.")
    private String academicTerm;
}