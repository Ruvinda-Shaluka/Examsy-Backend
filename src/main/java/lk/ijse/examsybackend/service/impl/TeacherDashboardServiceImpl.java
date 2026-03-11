package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.CourseCreateDTO;
import lk.ijse.examsybackend.dto.TeacherClassCardDTO;
import lk.ijse.examsybackend.entity.Course;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.repository.CourseRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl {

    private final CourseRepo courseRepository;
    private final TeacherRepo teacherRepository;

    @Transactional(readOnly = true)
    public List<TeacherClassCardDTO> getMyClasses(String username) {
        List<Course> courses = courseRepository.findByTeacherUserAccountUsernameAndIsArchivedFalse(username);

        return courses.stream().map(course ->
                TeacherClassCardDTO.builder()
                        .id(course.getId())
                        .title(course.getName())
                        .section(course.getSectionName())
                        .bannerColor(course.getThemeColorHex())
                        .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void deleteClass(String username, Integer courseId) {
        // Securely fetch the course to ensure this teacher actually owns it!
        Course course = courseRepository.findByIdAndTeacherUserAccountUsername(courseId, username)
                .orElseThrow(() -> new RuntimeException("Class not found or unauthorized access."));

        // Hard delete the course
        courseRepository.delete(course);
    }

    @Transactional
    public TeacherClassCardDTO createClass(String username, CourseCreateDTO dto) {
        Teacher teacher = teacherRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher profile not found."));

        String uniqueCode = UUID.randomUUID().toString().substring(0, 7).toUpperCase();

        String[] colors = {"#4F46E5", "#059669", "#DC2626", "#D97706", "#7C3AED", "#2563EB", "#0891B2"};
        String randomColor = colors[new Random().nextInt(colors.length)];

        Course newCourse = Course.builder()
                .teacher(teacher)
                .name(dto.getName())
                .sectionName(dto.getSectionName())
                .academicTerm(dto.getAcademicTerm())
                .classCode(uniqueCode)
                .themeColorHex(randomColor)
                .isArchived(false)
                .build();

        Course savedCourse = courseRepository.save(newCourse);

        return TeacherClassCardDTO.builder()
                .id(savedCourse.getId())
                .title(savedCourse.getName())
                .section(savedCourse.getSectionName())
                .bannerColor(savedCourse.getThemeColorHex())
                .build();
    }
}