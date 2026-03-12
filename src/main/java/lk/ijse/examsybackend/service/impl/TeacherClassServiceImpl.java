package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.TeacherClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherClassServiceImpl implements TeacherClassService {

    private final CourseRepo courseRepository; // Assuming you have this
    private final ClassAnnouncementRepo announcementRepository;
    private final UserAccountRepo userAccountRepository; // Assuming you have this

    @Transactional(readOnly = true)
    @Override
    public ClassStreamDTO getClassStream(Integer classId) {
        Course course = courseRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        List<AnnouncementDTO> announcementDTOs = announcementRepository.findByCourseIdOrderByCreatedAtDesc(classId)
                .stream().map(a -> AnnouncementDTO.builder()
                        .id(a.getId())
                        .authorName(a.getAuthor().getUsername()) // Or fetch Full Name if linked to Teacher entity
                        .content(a.getContent())
                        .formattedDate(a.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")))
                        .build())
                .collect(Collectors.toList());

        return ClassStreamDTO.builder()
                .classCode(course.getClassCode()) // Assuming your Course entity has a classCode field!
                .announcements(announcementDTOs)
                .build();
    }

    @Transactional
    @Override
    public AnnouncementDTO postAnnouncement(Integer classId, String username, CreateAnnouncementDTO dto) {
        Course course = courseRepository.findById(classId).orElseThrow();
        UserAccount author = userAccountRepository.findByUsername(username).orElseThrow();

        ClassAnnouncement announcement = ClassAnnouncement.builder()
                .course(course)
                .author(author)
                .content(dto.getContent())
                .build();

        announcement = announcementRepository.save(announcement);

        return AnnouncementDTO.builder()
                .id(announcement.getId())
                .authorName(author.getUsername())
                .content(announcement.getContent())
                .formattedDate("Just now")
                .build();
    }
}