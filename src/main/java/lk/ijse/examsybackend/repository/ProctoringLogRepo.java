package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ProctoringLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProctoringLogRepo extends JpaRepository<ProctoringLog,Integer> {
    List<ProctoringLog> findByExamSubmissionId(Integer submissionId);
}
