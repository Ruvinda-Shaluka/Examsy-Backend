package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    @PostMapping("signup/student")
    public ResponseEntity<APIResponse> registerStudent(@Valid @RequestBody StudentRegisterDTO dto) {
        return ResponseEntity.ok(new APIResponse(200, "OK", authService.registerStudent(dto)));
    }

    @PostMapping("signup/teacher")
    public ResponseEntity<APIResponse> registerTeacher(@Valid @RequestBody TeacherRegisterDTO dto) {
        return ResponseEntity.ok(new APIResponse(200, "OK", authService.registerTeacher(dto)));
    }

    @PostMapping("sign-in")
    public ResponseEntity<APIResponse> loginUser(@Valid @RequestBody AuthDTO authDTO) {
        return ResponseEntity.ok(new APIResponse(200, "OK", authService.authenticate(authDTO)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse> forgotPassword(@RequestBody ForgotPasswordDTO dto) {
        authService.initiatePasswordReset(dto.getEmail());
        return ResponseEntity.ok(new APIResponse(200, "Code sent successfully", null));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<APIResponse> verifyCode(@RequestBody VerifyCodeDTO dto) {
        authService.verifyResetCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(new APIResponse(200, "Code verified", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse> resetPassword(@RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
        return ResponseEntity.ok(new APIResponse(200, "Password updated successfully", null));
    }

    @PostMapping("/google")
    public ResponseEntity<APIResponse> authenticateGoogle(@RequestBody GoogleAuthDTO dto) {
        return ResponseEntity.ok(new APIResponse(200, "OK", authService.authenticateWithGoogle(dto)));
    }

}