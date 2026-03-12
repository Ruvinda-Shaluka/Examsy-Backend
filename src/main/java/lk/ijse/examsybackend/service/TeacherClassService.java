package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.AnnouncementDTO;
import lk.ijse.examsybackend.dto.ClassStreamDTO;
import lk.ijse.examsybackend.dto.CreateAnnouncementDTO;
import org.springframework.transaction.annotation.Transactional;

public interface TeacherClassService {
    @Transactional(readOnly = true)
    ClassStreamDTO getClassStream(Integer classId);

    @Transactional
    AnnouncementDTO postAnnouncement(Integer classId, String username, CreateAnnouncementDTO dto);
}
