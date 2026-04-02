package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.dto.response.ClassPeopleDTO;
import lk.ijse.examsybackend.dto.response.ClassStreamDTO;
import lk.ijse.examsybackend.dto.response.JoinRequestDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TeacherClassService {
    @Transactional(readOnly = true)
    ClassStreamDTO getClassStream(Integer classId);

    @Transactional
    AnnouncementDTO postAnnouncement(Integer classId, String username, CreateAnnouncementDTO dto);

    @Transactional
    AnnouncementDTO updateAnnouncement(Integer classId, Integer announcementId, String username, CreateAnnouncementDTO dto);

    @Transactional
    void deleteAnnouncement(Integer classId, Integer announcementId, String username);

    @Transactional
    void updateClassAppearance(Integer classId, String username, UpdateAppearanceDTO dto);

    @Transactional(readOnly = true)
    ClassPeopleDTO getClassPeople(Integer classId);

    @Transactional
    void removeStudentFromClass(String teacherUsername, Integer classId, Integer studentId);

    @Transactional
    void inviteStudent(String teacherUsername, Integer classId, InviteStudentDTO dto);

    @Transactional(readOnly = true)
    List<JoinRequestDTO> getPendingJoinRequests(String teacherUsername, Integer classId);

    @Transactional
    void approveJoinRequest(String teacherUsername, Integer requestId);

    @Transactional
    void rejectJoinRequest(String teacherUsername, Integer requestId);
}
