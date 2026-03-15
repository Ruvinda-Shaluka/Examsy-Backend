package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepo extends JpaRepository<ClassEnrollment,Integer> {

    // Find all classes a student is enrolled in using their token username
    List<ClassEnrollment> findByStudentUserAccountUsername(String username);

    // Find a specific enrollment to safely delete it
    Optional<ClassEnrollment> findByCourseIdAndStudentUserAccountUsername(Integer courseId, String username);

    List<ClassEnrollment> findByCourseId(Integer courseId);

    Optional<ClassEnrollment> findByCourseIdAndStudentId(Integer courseId, Integer studentId);

}
