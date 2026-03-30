package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamSubmissionRepo extends JpaRepository<ExamSubmission,Integer> {

    Optional<ExamSubmission> findByExamIdAndStudentId(Integer examId, Integer studentId);

    int countByExamIdAndStatus(Integer examId, String status);

    // Count students based on their progress status (e.g., "IN_PROGRESS" or "SUBMITTED")
    int countByExamIdAndStatusIn(Integer examId, List<String> status);

    // Fetch all student submissions for a specific exam
    List<ExamSubmission> findByExamId(Integer examId);

    List<ExamSubmission> findByStudentIdAndStatusOrderBySubmittedAtAsc(Integer studentId, String status);

}
