package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    AuthResponseDTO authenticate(AuthDTO authDTO);

    @Transactional
    String registerStudent(StudentRegisterDTO dto);

    @Transactional
    String registerTeacher(TeacherRegisterDTO dto);

    // Generate Code and Send Email
    @Transactional
    void initiatePasswordReset(String email);

    // Verify the Code
    boolean verifyResetCode(String email, String code);

    // Update the Password
    @Transactional
    void resetPassword(String email, String code, String newPassword);

    @Transactional
    AuthResponseDTO authenticateWithGoogle(GoogleAuthDTO dto);
}
