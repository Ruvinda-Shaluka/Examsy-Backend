package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubmissionRepo extends JpaRepository<ExamSubmission,Integer> {

    Optional<ExamSubmission> findByExamIdAndStudentId(Integer examId, Integer studentId);

    // Count students based on their progress status (e.g., "IN_PROGRESS" or "SUBMITTED")
    int countByExamIdAndStatus(Integer examId, String status);

    int countByExamIdAndStatusIn(Integer examId, List<String> status);

}
