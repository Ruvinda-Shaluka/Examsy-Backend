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
import org.springframework.transaction.annotation.Transactional; // Make sure this is imported!

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentDashboardServiceImpl implements StudentDashboardService {

    private final ClassEnrollmentRepo enrollmentRepository;
    private final CourseRepo courseRepository;
    private final StudentRepo studentRepository;
    private final ReportRepo reportRepository;

    // The Transactional annotation keeps the DB session open for Lazy loading!
    @Transactional(readOnly = true)
    @Override
    public List<StudentClassCardDTO> getMyEnrolledClasses(String username) {
        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentUserAccountUsername(username);

        return enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();

            // Using the Builder pattern is much safer here than ModelMapper for complex nested objects! and also compile time safety.
            return StudentClassCardDTO.builder()
                    .id(course.getId())
                    .title(course.getName())
                    .section(course.getSectionName())
                    .bannerColor(course.getThemeColorHex())
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

        // 1. Safely extract the Course ID from "https://examsy.com/join/6/req-w52tnt"
        try {
            String[] parts = link.split("/join/");
            if (parts.length < 2) throw new Exception();
            String idPart = parts[1].split("/")[0]; // Grabs the "6"
            courseId = Integer.parseInt(idPart);
        } catch (Exception e) {
            throw new RuntimeException("Invalid invite link format. Please check the link and try again.");
        }

        // 2. Find the Student
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student profile not found."));

        // 3. Find the Course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Class not found or the link has expired."));

        // 4. Check if already enrolled to prevent duplicates
        boolean alreadyEnrolled = enrollmentRepository
                .findByCourseIdAndStudentUserAccountUsername(courseId, username)
                .isPresent();

        if (alreadyEnrolled) {
            throw new RuntimeException("You are already enrolled in this class.");
        }

        // 5. Create and Save the Enrollment
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .course(course)
                .student(student)
                // enrolledAt is handled automatically by @CreationTimestamp
                .build();

        enrollmentRepository.save(enrollment);

        // 6. Return the mapped DTO so React can instantly show the new card
        return StudentClassCardDTO.builder()
                .id(course.getId())
                .title(course.getName())
                .section(course.getSectionName())
                .bannerColor(course.getThemeColorHex())
                .teacher(course.getTeacher() != null ? course.getTeacher().getFullName() : "Unknown Instructor")
                .build();
    }

    @Transactional
    @Override
    public void fileReport(String username, ReportCreateDTO dto) {
        // 1. Find the reporting student
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 2. Find the target course
        Course course = courseRepository.findById(dto.getTargetCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // 3. Map and save the Report
        Report report = Report.builder()
                .reporterStudent(student)
                .targetCourse(course)
                .category(dto.getCategory())
                .description(dto.getDescription())
                .priorityLevel(dto.getPriorityLevel())
                .status("PENDING") // Default status
                .build();

        reportRepository.save(report);
    }
}