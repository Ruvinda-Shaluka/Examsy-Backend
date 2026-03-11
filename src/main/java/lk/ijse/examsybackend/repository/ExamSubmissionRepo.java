package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExamSubmissionRepo extends JpaRepository<ExamSubmission,Integer> {

    Optional<ExamSubmission> findByExamIdAndStudentId(Integer examId, Integer studentId);

}
