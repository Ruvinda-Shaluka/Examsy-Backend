package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.APIResponse;
import lk.ijse.examsybackend.dto.AuthDTO;
import lk.ijse.examsybackend.dto.StudentRegisterDTO;
import lk.ijse.examsybackend.dto.TeacherRegisterDTO;
import lk.ijse.examsybackend.service.AuthService;
import lk.ijse.examsybackend.service.impl.AuthServiceImpl;
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
}