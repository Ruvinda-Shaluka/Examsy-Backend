package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.AuthDTO;
import lk.ijse.examsybackend.dto.AuthResponseDTO;
import lk.ijse.examsybackend.dto.StudentRegisterDTO;
import lk.ijse.examsybackend.dto.TeacherRegisterDTO;
import lk.ijse.examsybackend.entity.Role;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.entity.UserAccount;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.repository.UserAccountRepo;
import lk.ijse.examsybackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserAccountRepo userAccountRepository;
    private final StudentRepo studentRepository;
    private final TeacherRepo teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Authenticates a user (Student, Teacher, or Admin) and returns a JWT token.
     */
    public AuthResponseDTO authenticate(AuthDTO authDTO) {
        String identifier = authDTO.getUsername();
        // 1. Find user from DB
        UserAccount user = userAccountRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        // 2. Match passwords (DB hash vs Request password)
        if (!passwordEncoder.matches(authDTO.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials for user: " + identifier);
        }

        // 3. Generate new token
        String token = jwtUtil.generateToken(authDTO.getUsername());
        return new AuthResponseDTO(token, user.getRole().name());
    }

    /**
     * Registers a new Student.
     * @Transactional ensures that if saving the Student profile fails,
     * the UserAccount creation is rolled back automatically.
     */
    @Transactional
    public String registerStudent(StudentRegisterDTO dto) {
        validateNewUser(dto.getUsername(), dto.getEmail());

        // 1. Create and save the Base Authentication Account
        UserAccount account = UserAccount.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(Role.STUDENT)
                .authProvider("LOCAL")
                .isActive(true)
                .build();

        UserAccount savedAccount = userAccountRepository.save(account);

        // 2. Create and save the Student Profile linked to the new account
        Student student = Student.builder()
                .userAccount(savedAccount)
                .fullName(dto.getFullName())
                .studentIdentificationNumber(dto.getStudentIdentificationNumber())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .grade(dto.getGrade())
                // Default notification settings (optional, but good practice)
                .notifyEmail(true)
                .notifyPush(false)
                .notifyIdentity(true)
                .build();

        studentRepository.save(student);

        return "Student registered successfully";
    }

    /**
     * Registers a new Teacher.
     */
    @Transactional
    public String registerTeacher(TeacherRegisterDTO dto) {
        validateNewUser(dto.getUsername(), dto.getEmail());

        // 1. Create and save the Base Authentication Account
        UserAccount account = UserAccount.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(Role.TEACHER)
                .authProvider("LOCAL")
                .isActive(true)
                .build();

        UserAccount savedAccount = userAccountRepository.save(account);

        // 2. Create and save the Teacher Profile linked to the new account
        Teacher teacher = Teacher.builder()
                .userAccount(savedAccount)
                .fullName(dto.getFullName())
                .instructorId(dto.getInstructorId())
                .specialization(dto.getSpecialization())
                // Default notification settings
                .notifyEmail(true)
                .notifyPush(false)
                .notifySecurity(true)
                .build();

        teacherRepository.save(teacher);

        return "Teacher registered successfully";
    }

    /**
     * Helper method to keep validation logic DRY (Don't Repeat Yourself).
     */
    private void validateNewUser(String username, String email) {
        if (userAccountRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already in use");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new RuntimeException("Email is already in use");
        }
    }
}