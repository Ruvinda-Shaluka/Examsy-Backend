package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.AdminProfileDTO;
import lk.ijse.examsybackend.dto.AdminProfileUpdateDTO;
import lk.ijse.examsybackend.service.AdminProfileService;
import lk.ijse.examsybackend.service.impl.AdminProfileServiceImpl;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admins/me")
@RequiredArgsConstructor
@Validated
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    public ResponseEntity<APIResponse<AdminProfileDTO>> getProfile(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(new APIResponse<>(200, "Success", adminProfileService.getMyProfile(user.getUsername())));
    }

    @PutMapping
    public ResponseEntity<APIResponse<AdminProfileDTO>> updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody AdminProfileUpdateDTO dto) {

        AdminProfileDTO updatedProfile = adminProfileService.updateProfile(user.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Profile updated successfully", updatedProfile));
    }
}