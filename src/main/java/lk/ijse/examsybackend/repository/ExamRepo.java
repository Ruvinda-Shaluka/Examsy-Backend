package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepo extends JpaRepository<Exam,Integer> {
}
