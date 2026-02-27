package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepo extends JpaRepository<Student,Integer> {
}
