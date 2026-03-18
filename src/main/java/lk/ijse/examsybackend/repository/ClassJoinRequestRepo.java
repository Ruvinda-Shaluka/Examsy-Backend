package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ClassJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClassJoinRequestRepo extends JpaRepository<ClassJoinRequest, Integer> {

    // To check if a student already requested to join
    Optional<ClassJoinRequest> findByCourseIdAndStudentUserAccountUsername(Integer courseId, String username);
}