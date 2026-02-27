package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepo extends JpaRepository<Teacher,Integer> {
}
