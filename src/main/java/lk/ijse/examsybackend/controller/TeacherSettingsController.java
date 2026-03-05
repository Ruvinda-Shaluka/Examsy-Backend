package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.TeacherDTO;
import lk.ijse.examsybackend.service.TeacherService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherSettingsController {

    private final TeacherService teacherService;

    @GetMapping("/me")
    public ResponseEntity<APIResponse<TeacherDTO>> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        TeacherDTO profile = teacherService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Profile Fetched Successfully", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<APIResponse<TeacherDTO>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TeacherDTO updateData) {
        TeacherDTO updatedProfile = teacherService.updateMyProfile(userDetails.getUsername(), updateData);
        return ResponseEntity.ok(new APIResponse<>(200, "Profile Updated Successfully", updatedProfile));
    }
}