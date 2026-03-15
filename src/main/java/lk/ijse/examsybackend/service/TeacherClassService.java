package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import org.springframework.transaction.annotation.Transactional;

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
}
