package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.AuthDTO;
import lk.ijse.examsybackend.dto.AuthResponseDTO;
import lk.ijse.examsybackend.dto.StudentRegisterDTO;
import lk.ijse.examsybackend.dto.TeacherRegisterDTO;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    AuthResponseDTO authenticate(AuthDTO authDTO);

    @Transactional
    String registerStudent(StudentRegisterDTO dto);

    @Transactional
    String registerTeacher(TeacherRegisterDTO dto);
}
