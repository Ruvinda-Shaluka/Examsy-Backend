package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.AnnouncementDTO;
import lk.ijse.examsybackend.dto.ClassStreamDTO;
import lk.ijse.examsybackend.dto.CreateAnnouncementDTO;
import lk.ijse.examsybackend.service.TeacherClassService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teacher/classes")
@RequiredArgsConstructor
@Validated
public class TeacherClassController {

    private final TeacherClassService teacherClassService;

    // Fetch the Class Stream (Announcements + Class Code)
    @GetMapping("/{classId}/stream")
    public ResponseEntity<APIResponse<ClassStreamDTO>> getStream(@PathVariable Integer classId) {
        ClassStreamDTO streamData = teacherClassService.getClassStream(classId);
        return ResponseEntity.ok(new APIResponse<>(200, "Stream loaded successfully", streamData));
    }

    // Post a new Announcement to the Stream
    @PostMapping("/{classId}/announcements")
    public ResponseEntity<APIResponse<AnnouncementDTO>> postAnnouncement(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody @Valid CreateAnnouncementDTO dto) {

        // Passes the class ID, the teacher's username, and the secure payload to the service
        AnnouncementDTO newPost = teacherClassService.postAnnouncement(classId, user.getUsername(), dto);

        return ResponseEntity.ok(new APIResponse<>(200, "Announcement posted successfully", newPost));
    }
}