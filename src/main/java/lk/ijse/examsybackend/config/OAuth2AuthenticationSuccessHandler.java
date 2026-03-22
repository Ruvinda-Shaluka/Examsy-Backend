package lk.ijse.examsybackend.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.examsybackend.entity.Role;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.entity.UserAccount;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.repository.UserAccountRepo;
import lk.ijse.examsybackend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserAccountRepo userAccountRepository;
    private final StudentRepo studentRepository;
    private final TeacherRepo teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    @Transactional // 🟢 Ensure the entire save process happens in one transaction!
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String profilePictureUrl = oAuth2User.getAttribute("picture");

        // 🟢 Extract the requested role from the state parameter we customized
        String state = request.getParameter("state");
        String requestedRoleString = "student"; // default
        if (state != null && state.contains("||role:")) {
            requestedRoleString = state.substring(state.indexOf("||role:") + 7);
        }
        Role requestedRole = requestedRoleString.equalsIgnoreCase("teacher") ? Role.TEACHER : Role.STUDENT;

        // Generate a safe username
        String username = email.split("@")[0];

        // Check if user exists
        UserAccount userAccount = userAccountRepository.findByUsernameOrEmail(email, email).orElse(null);

        if (userAccount == null) {
            // AUTO-REGISTER NEW USER ACCOUNT
            userAccount = UserAccount.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .role(requestedRole)
                    .authProvider("GOOGLE")
                    .isActive(true)
                    .build();

            userAccount = userAccountRepository.save(userAccount);

            // CREATE THE SPECIFIC PROFILE
            if (requestedRole == Role.STUDENT) {
                Student student = Student.builder()
                        .userAccount(userAccount)
                        .fullName(name)
                        .studentIdentificationNumber(generateStudentIndexNumber())
                        .profilePictureUrl(profilePictureUrl)
                        .notifyEmail(true)
                        .notifyPush(true)
                        .notifyIdentity(true)
                        .build();
                studentRepository.save(student);

            } else {
                Teacher teacher = Teacher.builder()
                        .userAccount(userAccount)
                        .fullName(name)
                        .instructorId(generateCorporateInstructorId())
                        .profilePictureUrl(profilePictureUrl)
                        .notifyEmail(true)
                        .notifyPush(true)
                        .notifySecurity(true)
                        .build();
                teacherRepository.save(teacher);
            }
        } else {
            // UPDATE EXISTING USER (Your exact logic)
            if ("LOCAL".equals(userAccount.getAuthProvider())) {
                userAccount.setAuthProvider("GOOGLE_AND_LOCAL");
                userAccountRepository.save(userAccount);
            }

            if (userAccount.getRole() == Role.STUDENT) {
                Student student = studentRepository.findByUserAccount(userAccount).orElse(null);
                if (student != null) {
                    student.setProfilePictureUrl(profilePictureUrl);
                    studentRepository.save(student);
                }
            }

            if (userAccount.getRole() == Role.TEACHER) {
                Teacher teacher = teacherRepository.findByUserAccount(userAccount).orElse(null);
                if (teacher != null) {
                    teacher.setProfilePictureUrl(profilePictureUrl);
                    teacherRepository.save(teacher);
                }
            }
        }

        // Generate Examsy Token
        String jwtToken = jwtUtil.generateToken(userAccount.getUsername());
        String finalRole = userAccount.getRole().name();

        // Redirect to React
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", jwtToken)
                .queryParam("role", finalRole)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String generateStudentIndexNumber() {
        int year = java.time.Year.now().getValue();
        String uniqueHash = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        return "STU-" + year + "-" + uniqueHash;
    }

    private String generateCorporateInstructorId() {
        int year = java.time.Year.now().getValue();
        String uniqueHash = Long.toString(System.currentTimeMillis(), 36).toUpperCase();
        return "EMP-" + year + "-" + uniqueHash;
    }
}