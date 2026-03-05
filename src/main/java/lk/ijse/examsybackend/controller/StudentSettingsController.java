package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.StudentDTO;
import lk.ijse.examsybackend.service.StudentService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Validated
public class StudentSettingsController {

    private final StudentService studentService;

    // 1. Fetch current student profile
    @GetMapping("/me")
    public ResponseEntity<APIResponse<StudentDTO>> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {

        StudentDTO profile = studentService.getMyProfile(userDetails.getUsername());

        return ResponseEntity.ok(new APIResponse<>(
                200,
                "Profile Fetched Successfully",
                profile
        ));
    }

    // 2. Update the missing fields using the DTO
    @PutMapping("/me")
    public ResponseEntity<APIResponse<StudentDTO>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody StudentDTO updateData) {

        StudentDTO updatedProfile = studentService.updateMyProfile(userDetails.getUsername(), updateData);

        return ResponseEntity.ok(new APIResponse<>(
                200,
                "Profile Updated Successfully",
                updatedProfile
        ));
    }
}