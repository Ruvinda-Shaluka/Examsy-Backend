package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.dto.request.CreateAnnouncementDTO;
import lk.ijse.examsybackend.dto.request.InviteStudentDTO;
import lk.ijse.examsybackend.dto.request.UpdateAppearanceDTO;
import lk.ijse.examsybackend.dto.response.AnnouncementDTO;
import lk.ijse.examsybackend.dto.response.ClassPeopleDTO;
import lk.ijse.examsybackend.dto.response.ClassStreamDTO;
import lk.ijse.examsybackend.dto.response.JoinRequestDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.NotificationService;
import lk.ijse.examsybackend.service.TeacherClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final ClassJoinRequestRepo classJoinRequestRepo;
    private final JavaMailSender mailSender;

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

    @Transactional
    @Override
    public void inviteStudent(String teacherUsername, Integer classId, InviteStudentDTO dto) {
        // 1. Verify the teacher owns this class
        Course course = courseRepository.findByIdAndTeacherUserAccountUsername(classId, teacherUsername)
                .orElseThrow(() -> new RuntimeException("Class not found or unauthorized"));

        // 2. Grab the real, auto-rotating class code from the database
        String activeClassCode = course.getClassCode();

        // 3. Construct the secure join link
        String inviteLink = "https://examsy.com/join/" + classId + "/" + activeClassCode;

        // 4. Send the email
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new jakarta.mail.internet.InternetAddress("noreply@examsy.com", course.getTeacher().getFullName() + " (Examsy)"));
            helper.setTo(dto.getEmail());
            helper.setSubject("Invitation to join class: " + course.getName());

            String emailBody = "Hello,\n\n" +
                    "You have been invited by " + course.getTeacher().getFullName() +
                    " to join the class: " + course.getName() + ".\n\n" +
                    "Please copy the link below and paste it in your Examsy Student Dashboard to join:\n\n" +
                    inviteLink + "\n\n" +
                    "Welcome to the class!";

            helper.setText(emailBody);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("Email Error: " + e.getMessage());
            throw new RuntimeException("Failed to send invite email. Please check the email address and try again.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<JoinRequestDTO> getPendingJoinRequests(String teacherUsername, Integer classId) {
        // Verify teacher owns course
        courseRepository.findByIdAndTeacherUserAccountUsername(classId, teacherUsername)
                .orElseThrow(() -> new RuntimeException("Unauthorized"));

        return classJoinRequestRepo.findByCourseIdAndStatusOrderByRequestedAtAsc(classId, "PENDING")
                .stream().map(req -> JoinRequestDTO.builder()
                        .requestId(req.getId())
                        .studentId(req.getStudent().getId())
                        .studentName(req.getStudent().getFullName())
                        .studentEmail(req.getStudent().getUserAccount().getEmail())
                        .initial(req.getStudent().getFullName().substring(0, 1).toUpperCase())
                        .requestedAt(req.getRequestedAt())
                        .build()
                ).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void approveJoinRequest(String teacherUsername, Integer requestId) {
        ClassJoinRequest request = classJoinRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // Verify teacher owns this specific course
        if (!request.getCourse().getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized");
        }

        // 1. Create the official Enrollment!
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .course(request.getCourse())
                .student(request.getStudent())
                .build();
        classEnrollmentRepo.save(enrollment);

        // 2. Delete the pending request so it disappears from the waiting room
        classJoinRequestRepo.delete(request);

        // 3. Notify the student
        notificationService.notifyStudentOfJoinResult(request.getStudent(), request.getCourse(), true);
    }

    @Transactional
    @Override
    public void rejectJoinRequest(String teacherUsername, Integer requestId) {
        ClassJoinRequest request = classJoinRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!request.getCourse().getTeacher().getUserAccount().getUsername().equals(teacherUsername)) {
            throw new RuntimeException("Unauthorized");
        }

        // Delete the request
        classJoinRequestRepo.delete(request);

        // Notify the student they were rejected
        notificationService.notifyStudentOfJoinResult(request.getStudent(), request.getCourse(), false);
    }
}