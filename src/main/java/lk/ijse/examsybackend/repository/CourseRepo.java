package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepo extends JpaRepository<Course,Integer> {
}
