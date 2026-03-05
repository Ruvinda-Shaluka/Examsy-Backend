package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.TeacherClassCardDTO;
import lk.ijse.examsybackend.entity.Course;
import lk.ijse.examsybackend.repository.CourseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    private final CourseRepo courseRepository;

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
}