package lk.ijse.examsybackend.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.entity.Role;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.entity.UserAccount;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.repository.UserAccountRepo;
import lk.ijse.examsybackend.service.AuthService;
import lk.ijse.examsybackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepo userAccountRepository;
    private final StudentRepo studentRepository;
    private final TeacherRepo teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * Authenticates a user (Student, Teacher, or Admin) and returns a JWT token.
     */
    @Override
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
    @Override
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
    @Override
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

    // Generate Code and Send Email
    @Transactional
    @Override
    public void initiatePasswordReset(String email) {
        UserAccount user = userAccountRepository.findByUsernameOrEmail(email, email)
                .orElseThrow(() -> new RuntimeException("If this email exists, a code has been sent.")); // Vague message for security!

        // Generate 6-digit code
        String code = String.format("%06d", new java.util.Random().nextInt(999999));

        user.setResetCode(code);
        user.setResetCodeExpiresAt(LocalDateTime.now().plusMinutes(5)); // Expires in 5 mins
        userAccountRepository.save(user);

        // Send Email (Assuming you have mailSender configured)
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("Examsy Password Reset Code");
            helper.setText("Your password reset code is: " + code + "\n\nThis code expires in 15 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email");
        }
    }

    // Verify the Code
    @Override
    public boolean verifyResetCode(String email, String code) {
        UserAccount user = userAccountRepository.findByUsernameOrEmail(email, email)
                .orElseThrow(() -> new RuntimeException("Invalid request"));

        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            throw new RuntimeException("Invalid verification code");
        }
        if (LocalDateTime.now().isAfter(user.getResetCodeExpiresAt())) {
            throw new RuntimeException("Verification code has expired");
        }
        return true;
    }

    // Update the Password
    @Transactional
    @Override
    public void resetPassword(String email, String code, String newPassword) {
        // Re-verify the code just in case they bypassed the frontend
        verifyResetCode(email, code);

        UserAccount user = userAccountRepository.findByUsernameOrEmail(email, email).get();

        // Hash the new password and clear the reset code!
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpiresAt(null);

        userAccountRepository.save(user);
    }

    @Transactional
    @Override
    public AuthResponseDTO authenticateWithGoogle(GoogleAuthDTO dto) {
        try {
            // 1. Verify the Google Token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(dto.getToken());
            if (idToken == null) {
                throw new RuntimeException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 🟢 NEW: Extract the profile picture URL from the Google payload
            String profilePictureUrl = (String) payload.get("picture");

            // Generate a safe username from the email
            String username = email.split("@")[0];

            // 2. Check if the user already exists in Examsy
            UserAccount userAccount = userAccountRepository.findByUsernameOrEmail(email, email).orElse(null);

            if (userAccount == null) {
                // 3. User does not exist, auto-register them based on the requested role
                Role requestedRole = dto.getRole().equalsIgnoreCase("teacher") ? Role.TEACHER : Role.STUDENT;

                userAccount = UserAccount.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString())) // Random safe password
                        .role(requestedRole)
                        .authProvider("GOOGLE")
                        .isActive(true)
                        .build();

                userAccount = userAccountRepository.save(userAccount);

                // Create the specific profile (Student or Teacher)
                if (requestedRole == Role.STUDENT) {

                    String uniqueId = generateStudentIndexNumber();

                    Student student = Student.builder()
                            .userAccount(userAccount)
                            .fullName(name)
                            .studentIdentificationNumber(uniqueId)
                            .profilePictureUrl(profilePictureUrl)
                            .notifyEmail(true)
                            .notifyPush(true)
                            .notifyIdentity(true)
                            .build();
                    studentRepository.save(student);

                } else {

                    String uniqueId = generateCorporateInstructorId();

                    Teacher teacher = Teacher.builder()
                            .userAccount(userAccount)
                            .fullName(name)
                            .instructorId(uniqueId)
                            .profilePictureUrl(profilePictureUrl)
                            .notifyEmail(true)
                            .notifyPush(true)
                            .notifySecurity(true)
                            .build();
                    teacherRepository.save(teacher);
                }
            } else {
                // If they exist but registered locally, optionally update authProvider to "GOOGLE_AND_LOCAL"
                if ("LOCAL".equals(userAccount.getAuthProvider())) {
                    userAccount.setAuthProvider("GOOGLE_AND_LOCAL");
                    userAccountRepository.save(userAccount);
                }

                 //If you want to update student picture every time they log in via Google
                 if (userAccount.getRole() == Role.STUDENT) {
                     Student student = studentRepository.findByUserAccount(userAccount).get();
                     student.setProfilePictureUrl(profilePictureUrl);
                     studentRepository.save(student);
                 }

                //If you want to update teacher picture every time they log in via Google
                if (userAccount.getRole() == Role.TEACHER) {
                    Teacher teacher = teacherRepository.findByUserAccount(userAccount).get();
                    teacher.setProfilePictureUrl(profilePictureUrl);
                    teacherRepository.save(teacher);
                }

            }

            // 4. Generate Examsy JWT Token for the verified user
            String token = jwtUtil.generateToken(userAccount.getUsername());
            return new AuthResponseDTO(token, userAccount.getRole().name());

        } catch (Exception e) {
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    // Helper function to generate a unique Student Index Number matching frontend logic
    private String generateStudentIndexNumber() {
        int year = java.time.Year.now().getValue();
        String uniqueHash = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        return "STU-" + year + "-" + uniqueHash;
    }

    // Helper function to generate the Corporate Format ID matching frontend logic
    private String generateCorporateInstructorId() {
        int year = java.time.Year.now().getValue();
        String uniqueHash = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        return "EMP-" + year + "-" + uniqueHash;
    }
}