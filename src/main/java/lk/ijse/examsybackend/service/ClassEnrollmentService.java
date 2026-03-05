package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.StudentClassCardDTO;
import lk.ijse.examsybackend.entity.ClassEnrollment;
import lk.ijse.examsybackend.repository.ClassEnrollmentRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClassEnrollmentService {

    private final ClassEnrollmentRepo enrollmentRepository;
    private final ModelMapper modelMapper;

    public List<StudentClassCardDTO> getMyEnrolledClasses(String username) {
        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentUserAccountUsername(username);

        return enrollments.stream().map(enrollment -> {
            // Map the base course details
            StudentClassCardDTO dto = modelMapper.map(enrollment.getCourse(), StudentClassCardDTO.class);

            // Explicitly map the fields that have different names in React vs Entity
            dto.setTitle(enrollment.getCourse().getName());
            dto.setSection(enrollment.getCourse().getSectionName());
            dto.setBannerColor(enrollment.getCourse().getThemeColorHex());

            // Map the nested teacher name safely
            if (enrollment.getCourse().getTeacher() != null) {
                dto.setTeacher(enrollment.getCourse().getTeacher().getFullName());
            }
            return dto;
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
