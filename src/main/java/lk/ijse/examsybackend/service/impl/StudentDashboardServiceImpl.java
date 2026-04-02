package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.*;
import lk.ijse.examsybackend.dto.response.CalendarExamDTO;
import lk.ijse.examsybackend.dto.response.ClassPeopleDTO;
import lk.ijse.examsybackend.dto.response.StudentClassCardDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lk.ijse.examsybackend.service.NotificationService;
import lk.ijse.examsybackend.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private final ClassEnrollmentRepo enrollmentRepository;
    private final CourseRepo courseRepository;
    private final StudentRepo studentRepository;
    private final ReportRepo reportRepository;
    private final ClassEnrollmentRepo classEnrollmentRepo;
    private final ExamRepo examRepository;
    private final ClassJoinRequestRepo classJoinRequestRepo;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    @Override
    public List<StudentClassCardDTO> getMyEnrolledClasses(String username) {
        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentUserAccountUsername(username);

        return enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();

            return StudentClassCardDTO.builder()
                    .id(course.getId())
                    .title(course.getName())
                    .section(course.getSectionName())
                    .themeColorHex(course.getThemeColorHex())
                    .bannerImageUrl(course.getBannerImageUrl())
                    .teacher(course.getTeacher() != null ? course.getTeacher().getFullName() : "Unknown Instructor")
                    .build();

        }).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void unenrollFromClass(String username, Integer courseId) {
        ClassEnrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentUserAccountUsername(courseId, username)
                .orElseThrow(() -> new RuntimeException("Enrollment not found or unauthorized access."));

        enrollmentRepository.delete(enrollment);
    }

    @Transactional
    @Override
    public String joinClass(String username, JoinClassDTO dto) {
        String link = dto.getInviteLink().trim();
        Integer courseId;
        String extractedCode;

        // 1. Safely extract the Course ID AND the Invite Code
        try {
            String[] parts = link.split("/join/");
            if (parts.length < 2) throw new Exception();
            String[] params = parts[1].split("/");
            if (params.length < 2) throw new Exception();
            courseId = Integer.parseInt(params[0]);
            extractedCode = params[1].trim();
        } catch (Exception e) {
            throw new RuntimeException("Invalid invite link format.");
        }

        // 2. Find Student and Course
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found."));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Class not found."));

        // 3. Security Check
        if (course.getClassCode() == null || !course.getClassCode().equals(extractedCode)) {
            throw new RuntimeException("Invalid or expired invite link.");
        }

        // 4. Check if already enrolled
        if (enrollmentRepository.findByCourseIdAndStudentUserAccountUsername(courseId, username).isPresent()) {
            throw new RuntimeException("You are already enrolled in this class.");
        }

        // 5.Check if already requested
        if (classJoinRequestRepo.findByCourseIdAndStudentUserAccountUsername(courseId, username).isPresent()) {
            throw new RuntimeException("You have already sent a request to join this class. Please wait for the instructor to approve it.");
        }

        // 6.Save the PENDING request instead of Enrollment
        ClassJoinRequest request = ClassJoinRequest.builder()
                .course(course)
                .student(student)
                .status("PENDING")
                .build();
        classJoinRequestRepo.save(request);

        // 7. Alert the Teacher!
        notificationService.notifyTeacherOfJoinRequest(course, student);

        // 8. Return a success message
        return "Request sent successfully! You will be notified once the instructor approves your request.";
    }

    @Transactional
    @Override
    public void fileReport(String username, ReportCreateDTO dto) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Course course = courseRepository.findById(dto.getTargetCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Report report = Report.builder()
                .reporterStudent(student)
                .targetCourse(course)
                .category(dto.getCategory())
                .description(dto.getDescription())
                .priorityLevel(dto.getPriorityLevel())
                .status("PENDING")
                .build();

        reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    @Override
    public ClassPeopleDTO getClassPeople(Integer classId) {
        // 1. Find the Course
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

        // 3. Find all students enrolled in this class
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
                    .profileImageUrl(student.getProfilePictureUrl())
                    .build();
        }).collect(Collectors.toList());

        // 5. Return the packaged DTO
        return ClassPeopleDTO.builder()
                .teachers(List.of(teacherDto))
                .students(studentDtos)
                .build();
    }


    @Transactional(readOnly = true)
    @Override
    public List<CalendarExamDTO> getStudentCalendarExams(String username) {
        List<Exam> exams = examRepository.findExamsByStudentUsername(username);
        return mapExamsToCalendarDTOs(exams); // Use the same mapping logic as above!
    }

    // Helper method to map entities to DTOs
    private List<CalendarExamDTO> mapExamsToCalendarDTOs(List<Exam> exams) {
        return exams.stream().map(exam -> {
            // Determine the calendar date based on Exam Mode
            LocalDateTime displayDate = "REAL_TIME".equals(exam.getExamMode()) ?
                    exam.getScheduledStartTime() : exam.getDeadlineTime();

            return CalendarExamDTO.builder()
                    .id(exam.getId())
                    .classId(exam.getCourse().getId())
                    .title(exam.getTitle())
                    .courseName(exam.getCourse().getName())
                    .themeColorHex(exam.getCourse().getThemeColorHex())
                    .examDate(displayDate)
                    .examMode(exam.getExamMode())
                    .build();
        }).collect(java.util.stream.Collectors.toList());
    }
}