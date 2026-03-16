package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.CalendarExamDTO;
import lk.ijse.examsybackend.dto.CourseCreateDTO;
import lk.ijse.examsybackend.dto.TeacherClassCardDTO;
import lk.ijse.examsybackend.entity.Course;
import lk.ijse.examsybackend.entity.Exam;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.repository.CourseRepo;
import lk.ijse.examsybackend.repository.ExamRepo;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.service.TeacherDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherDashboardServiceImpl implements TeacherDashboardService {

    private final CourseRepo courseRepository;
    private final TeacherRepo teacherRepository;
    private final ExamRepo examRepository;

    @Transactional(readOnly = true)
    @Override
    public List<TeacherClassCardDTO> getMyClasses(String username) {
        List<Course> courses = courseRepository.findByTeacherUserAccountUsernameAndIsArchivedFalse(username);

        return courses.stream().map(course ->
                TeacherClassCardDTO.builder()
                        .id(course.getId())
                        .title(course.getName())
                        .section(course.getSectionName())
                        .themeColorHex(course.getThemeColorHex())
                        .bannerImageUrl(course.getBannerImageUrl())
                        .classCode(course.getClassCode())
                        .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deleteClass(String username, Integer courseId) {
        // Securely fetch the course to ensure this teacher actually owns it!
        Course course = courseRepository.findByIdAndTeacherUserAccountUsername(courseId, username)
                .orElseThrow(() -> new RuntimeException("Class not found or unauthorized access."));

        // Hard delete the course
        courseRepository.delete(course);
    }

    @Transactional
    @Override
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
                .classCodeUpdatedAt(LocalDateTime.now())
                .isArchived(false)
                .build();

        Course savedCourse = courseRepository.save(newCourse);

        return TeacherClassCardDTO.builder()
                .id(savedCourse.getId())
                .title(savedCourse.getName())
                .section(savedCourse.getSectionName())
                .themeColorHex(savedCourse.getThemeColorHex())
                .bannerImageUrl(savedCourse.getBannerImageUrl())
                .classCode(savedCourse.getClassCode())
                .build();
    }

    @Transactional
    @Override
    public void rotateExpiredClassCodes(String username) {
        Teacher teacher = teacherRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        // Fetch all classes for this teacher
        List<Course> courses = courseRepository.findByTeacherId(teacher.getId());

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        for (Course course : courses) {
            // Fallback to createdAt if for some reason updated_at is null
            LocalDateTime lastUpdated = course.getClassCodeUpdatedAt() != null ?
                    course.getClassCodeUpdatedAt() : course.getCreatedAt();

            // If the code is older than 7 days, generate a new one!
            if (lastUpdated != null && lastUpdated.isBefore(sevenDaysAgo)) {
                String newCode = java.util.UUID.randomUUID().toString().substring(0, 7).toUpperCase();
                course.setClassCode(newCode);
                course.setClassCodeUpdatedAt(LocalDateTime.now()); // Reset the 7-day timer
                courseRepository.save(course);
            }
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<CalendarExamDTO> getTeacherCalendarExams(String username) {
        List<Exam> exams = examRepository.findByCourseTeacherUserAccountUsername(username);
        return mapExamsToCalendarDTOs(exams);
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