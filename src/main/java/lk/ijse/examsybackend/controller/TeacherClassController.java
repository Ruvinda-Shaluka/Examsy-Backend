package lk.ijse.examsybackend.controller;

import jakarta.validation.Valid;
import lk.ijse.examsybackend.dto.*;
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

        AnnouncementDTO newPost = teacherClassService.postAnnouncement(classId, user.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Announcement posted successfully", newPost));
    }

    @PutMapping("/{classId}/announcements/{announcementId}")
    public ResponseEntity<APIResponse<AnnouncementDTO>> updateAnnouncement(
            @PathVariable Integer classId,
            @PathVariable Integer announcementId,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody @Valid CreateAnnouncementDTO dto) {

        AnnouncementDTO updatedPost = teacherClassService.updateAnnouncement(classId, announcementId, user.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Announcement updated", updatedPost));
    }

    @DeleteMapping("/{classId}/announcements/{announcementId}")
    public ResponseEntity<APIResponse<Void>> deleteAnnouncement(
            @PathVariable Integer classId,
            @PathVariable Integer announcementId,
            @AuthenticationPrincipal UserDetails user) {

        teacherClassService.deleteAnnouncement(classId, announcementId, user.getUsername());
        return ResponseEntity.ok(new APIResponse<>(200, "Announcement deleted", null));
    }

    @PutMapping("/{classId}/appearance")
    public ResponseEntity<APIResponse<Void>> updateAppearance(
            @PathVariable Integer classId,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody UpdateAppearanceDTO dto) {

        teacherClassService.updateClassAppearance(classId, user.getUsername(), dto);
        return ResponseEntity.ok(new APIResponse<>(200, "Appearance updated successfully", null));
    }

    @GetMapping("/{classId}/people")
    public ResponseEntity<APIResponse<ClassPeopleDTO>> getClassPeople(@PathVariable Integer classId) {
        ClassPeopleDTO people = teacherClassService.getClassPeople(classId);
        return ResponseEntity.ok(new APIResponse<>(200, "Roster loaded", people));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public ResponseEntity<APIResponse<Void>> removeStudent(
            @PathVariable Integer classId,
            @PathVariable Integer studentId,
            @AuthenticationPrincipal UserDetails user) {

        teacherClassService.removeStudentFromClass(user.getUsername(), classId, studentId);
        return ResponseEntity.ok(new APIResponse<>(200, "Student successfully removed from class", null));
    }
}