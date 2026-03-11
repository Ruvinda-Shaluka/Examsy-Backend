package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.ProctoringDTO;
import lk.ijse.examsybackend.entity.*;
import lk.ijse.examsybackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentExamService {

    private final ExamSubmissionRepo submissionRepository;
    private final ProctoringLogRepo proctoringLogRepository;
    private final StudentRepo studentRepository;

    @Transactional
    public void logSecurityViolation(String username, ProctoringDTO dto) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Find their active exam submission
        ExamSubmission submission = submissionRepository.findByExamIdAndStudentId(dto.getExamId(), student.getId())
                .orElseThrow(() -> new RuntimeException("Active exam session not found"));

        // Update the main submission record
        submission.setSuspiciousEventCount(submission.getSuspiciousEventCount() + 1);
        submission.setTotalTimeAwaySeconds(submission.getTotalTimeAwaySeconds() + dto.getDurationSeconds());
        submission.setProctoringStatus("FLAGGED");
        submission.setLastKnownAction(dto.getEventType());

        submissionRepository.save(submission);

        // Save the detailed log for the Admin/Teacher to review later
        ProctoringLog log = ProctoringLog.builder()
                .examSubmission(submission)
                .eventType(dto.getEventType())
                .durationSeconds(dto.getDurationSeconds())
                .build();

        proctoringLogRepository.save(log);
    }
}