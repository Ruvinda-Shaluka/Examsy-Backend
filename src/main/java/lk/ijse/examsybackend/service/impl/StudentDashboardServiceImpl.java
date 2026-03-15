package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.JoinClassDTO;
import lk.ijse.examsybackend.dto.ReportCreateDTO;
import lk.ijse.examsybackend.dto.StudentClassCardDTO;
import lk.ijse.examsybackend.entity.ClassEnrollment;
import lk.ijse.examsybackend.entity.Course;
import lk.ijse.examsybackend.entity.Report;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.repository.ClassEnrollmentRepo;
import lk.ijse.examsybackend.repository.CourseRepo;
import lk.ijse.examsybackend.repository.ReportRepo;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.service.StudentDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private final ClassEnrollmentRepo enrollmentRepository;
    private final CourseRepo courseRepository;
    private final StudentRepo studentRepository;
    private final ReportRepo reportRepository;

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
    public StudentClassCardDTO joinClass(String username, JoinClassDTO dto) {
        String link = dto.getInviteLink().trim();
        Integer courseId;
        String extractedCode;

        // 1. Safely extract the Course ID AND the Invite Code from the URL
        try {
            String[] parts = link.split("/join/");
            if (parts.length < 2) throw new Exception();

            String[] params = parts[1].split("/");
            if (params.length < 2) throw new Exception(); // Ensure both ID and Code exist

            courseId = Integer.parseInt(params[0]);
            extractedCode = params[1].trim();
        } catch (Exception e) {
            throw new RuntimeException("Invalid invite link format. Please check the link and try again.");
        }

        // 2. Find the Student
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student profile not found."));

        // 3. Find the Course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Class not found. Verify the link."));

        // 4. SECURITY VALIDATION: Check if the link code matches the database code
        if (course.getClassCode() == null || !course.getClassCode().equals(extractedCode)) {
            throw new RuntimeException("Invalid or expired invite link. Please ask your instructor for the latest link.");
        }

        // 5. Check if already enrolled to prevent duplicates
        boolean alreadyEnrolled = enrollmentRepository
                .findByCourseIdAndStudentUserAccountUsername(courseId, username)
                .isPresent();

        if (alreadyEnrolled) {
            throw new RuntimeException("You are already enrolled in this class.");
        }

        // 6. Create and Save the Enrollment
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .course(course)
                .student(student)
                // If you have an enrolledAt timestamp in your entity, you can set it here:
                // .enrolledAt(LocalDateTime.now())
                .build();

        enrollmentRepository.save(enrollment);

        // 7. Return the mapped DTO
        return StudentClassCardDTO.builder()
                .id(course.getId())
                .title(course.getName())
                .section(course.getSectionName())
                .themeColorHex(course.getThemeColorHex())
                .bannerImageUrl(course.getBannerImageUrl())
                .teacher(course.getTeacher() != null ? course.getTeacher().getFullName() : "Unknown Instructor")
                .build();
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
}