package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.NotificationService;
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

    private final CourseRepo courseRepository;
    private final ClassAnnouncementRepo announcementRepository;
    private final UserAccountRepo userAccountRepository;

    //INJECT THE NOTIFICATION SERVICE
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    @Override
    public ClassStreamDTO getClassStream(Integer classId) {
        Course course = courseRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        List<AnnouncementDTO> announcementDTOs = announcementRepository.findByCourseIdOrderByCreatedAtDesc(classId)
                .stream().map(a -> AnnouncementDTO.builder()
                        .id(a.getId())
                        .authorName(a.getAuthor().getUsername())
                        .content(a.getContent())
                        .formattedDate(a.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")))
                        .build())
                .collect(Collectors.toList());

        return ClassStreamDTO.builder()
                .classCode(course.getClassCode())
                .announcements(announcementDTOs)
                .build();
    }

    @Transactional
    @Override
    public AnnouncementDTO postAnnouncement(Integer classId, String username, CreateAnnouncementDTO dto) {
        Course course = courseRepository.findById(classId).orElseThrow();
        UserAccount author = userAccountRepository.findByUsername(username).orElseThrow();

        // 1. Save the public broadcast announcement to the Stream
        ClassAnnouncement announcement = ClassAnnouncement.builder()
                .course(course)
                .author(author)
                .content(dto.getContent())
                .build();

        announcement = announcementRepository.save(announcement);

        // TRIGGER THE FAN-OUT NOTIFICATIONS
        // This offloads all the looping and preference-checking to the NotificationService!
        notificationService.dispatchAnnouncementNotifications(
                course.getId(),
                course.getClassCode(),
                author.getUsername(),
                dto.getContent()
        );

        return AnnouncementDTO.builder()
                .id(announcement.getId())
                .authorName(author.getUsername())
                .content(announcement.getContent())
                .formattedDate("Just now")
                .build();
    }

    @Transactional
    @Override
    public AnnouncementDTO updateAnnouncement(Integer classId, Integer announcementId, String username, CreateAnnouncementDTO dto) {
        ClassAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        // Security checks
        if (!announcement.getCourse().getId().equals(classId)) {
            throw new RuntimeException("Announcement does not belong to this class");
        }
        if (!announcement.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to edit this announcement");
        }

        announcement.setContent(dto.getContent());
        announcement = announcementRepository.save(announcement);

        return AnnouncementDTO.builder()
                .id(announcement.getId())
                .authorName(announcement.getAuthor().getUsername())
                .content(announcement.getContent())
                .formattedDate(announcement.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")) + " (Edited)")
                .build();
    }

    @Transactional
    @Override
    public void deleteAnnouncement(Integer classId, Integer announcementId, String username) {
        ClassAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        if (!announcement.getCourse().getId().equals(classId)) {
            throw new RuntimeException("Announcement does not belong to this class");
        }
        if (!announcement.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to delete this announcement");
        }

        announcementRepository.delete(announcement);
    }

    @Transactional
    @Override
    public void updateClassAppearance(Integer classId, String username, UpdateAppearanceDTO dto) {
        Course course = courseRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Security: Ensure only the teacher who created the class can edit it
        if (!course.getTeacher().getUserAccount().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to modify this class");
        }

        course.setThemeColorHex(dto.getThemeColorHex());
        course.setBannerImageUrl(dto.getBannerImageUrl());

        courseRepository.save(course);
    }
}