package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepo extends JpaRepository<ExamSubmission,Integer> {
}
