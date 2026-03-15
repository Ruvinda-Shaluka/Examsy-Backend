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
    private final NotificationService notificationService;
    private final ClassEnrollmentRepo classEnrollmentRepo;

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
                .title(course.getName())
                .section(course.getSectionName())
                .themeColorHex(course.getThemeColorHex())
                .bannerImageUrl(course.getBannerImageUrl())
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

        notificationService.dispatchAnnouncementNotifications(
                course.getId(),
                course.getName(), // Changed to Class Name for better emails
                course.getTeacher().getFullName(), // Use real Teacher Name!
                dto.getContent(),
                false // isUpdate = false
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

        if (!announcement.getCourse().getId().equals(classId)) {
            throw new RuntimeException("Announcement does not belong to this class");
        }
        if (!announcement.getAuthor().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to edit this announcement");
        }

        announcement.setContent(dto.getContent());
        announcement = announcementRepository.save(announcement);

        notificationService.dispatchAnnouncementNotifications(
                announcement.getCourse().getId(),
                announcement.getCourse().getName(),
                announcement.getCourse().getTeacher().getFullName(), // Real Teacher Name
                dto.getContent(),
                true // isUpdate = true
        );

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

    @Transactional(readOnly = true)
    @Override
    public ClassPeopleDTO getClassPeople(Integer classId) {
        // 1. Get the course to find the teacher
        Course course = courseRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Teacher teacher = course.getTeacher();

        // 2. Map the Teacher into a PersonDTO
        PersonDTO teacherDto = PersonDTO.builder()
                .id(teacher.getId())
                .name(teacher.getFullName())
                .email(teacher.getUserAccount().getEmail())
                .initial(teacher.getFullName().substring(0, 1).toUpperCase())
                .role("Teacher")
                .profileImageUrl(teacher.getProfilePictureUrl())
                .build();

        // 3. Find all active enrollments for this class
        List<ClassEnrollment> enrollments = classEnrollmentRepo.findByCourseId(classId);

        // 4. Map the Students into PersonDTOs
        List<PersonDTO> studentDtos = enrollments.stream().map(enrollment -> {
            Student student = enrollment.getStudent();
            return PersonDTO.builder()
                    .id(student.getId())
                    .name(student.getFullName())
                    .email(student.getUserAccount().getEmail())
                    .initial(student.getFullName().substring(0, 1).toUpperCase())
                    .role("Student")
                    // 🟢 Map the image URL here
                    .profileImageUrl(student.getProfilePictureUrl())
                    .build();
        }).collect(Collectors.toList());

        // 5. Package it all up and send it to React
        return ClassPeopleDTO.builder()
                .teachers(List.of(teacherDto))
                .students(studentDtos)
                .build();
    }

    @Transactional
    @Override
    public void removeStudentFromClass(String teacherUsername, Integer classId, Integer studentId) {
        // 1. Verify the teacher actually owns this class
        Course course = courseRepository.findByIdAndTeacherUserAccountUsername(classId, teacherUsername)
                .orElseThrow(() -> new RuntimeException("Class not found or unauthorized"));

        // 2. Find the exact enrollment record
        ClassEnrollment enrollment = classEnrollmentRepo.findByCourseIdAndStudentId(classId, studentId)
                .orElseThrow(() -> new RuntimeException("Student is not enrolled in this class"));

        // 3. Remove the student
        classEnrollmentRepo.delete(enrollment);
    }
}