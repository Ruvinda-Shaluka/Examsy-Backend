package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CourseRepo extends JpaRepository<Course,Integer> {

    // Find all active classes taught by this specific teacher
    List<Course> findByTeacherUserAccountUsernameAndIsArchivedFalse(String username);

    // Find a specific class to ensure the teacher actually owns it before deleting
    Optional<Course> findByIdAndTeacherUserAccountUsername(Integer id, String username);

}
