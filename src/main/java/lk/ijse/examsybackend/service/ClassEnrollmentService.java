package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.StudentClassCardDTO;
import lk.ijse.examsybackend.entity.ClassEnrollment;
import lk.ijse.examsybackend.entity.Course;
import lk.ijse.examsybackend.repository.ClassEnrollmentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Make sure this is imported!

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassEnrollmentService {

    private final ClassEnrollmentRepo enrollmentRepository;

    // 🛡️ The Transactional annotation keeps the DB session open for Lazy loading!
    @Transactional(readOnly = true)
    public List<StudentClassCardDTO> getMyEnrolledClasses(String username) {
        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentUserAccountUsername(username);

        return enrollments.stream().map(enrollment -> {
            Course course = enrollment.getCourse();

            // Using the Builder pattern is much safer here than ModelMapper
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
    public void unenrollFromClass(String username, Integer courseId) {
        ClassEnrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentUserAccountUsername(courseId, username)
                .orElseThrow(() -> new RuntimeException("Enrollment not found or unauthorized access."));

        enrollmentRepository.delete(enrollment);
    }
}