package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.MockExam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockExamRepo extends JpaRepository<MockExam,Integer> {
}
