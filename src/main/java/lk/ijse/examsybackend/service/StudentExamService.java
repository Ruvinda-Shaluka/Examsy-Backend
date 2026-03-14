package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.*;
import org.springframework.transaction.annotation.Transactional;

public interface StudentExamService {
    // --- 1. PROCTORING: Track Cheating / Tab Switches ---
    @Transactional
    void logSecurityViolation(String username, ProctoringDTO dto);

    // --- 2. EXAM START: Get Exam Data for React (Without Correct Answers!) ---
    @Transactional
    StudentExamViewDTO getExamForStudent(String username, Integer examId);

    // --- 3. EXAM SUBMIT: Grade Answers Instantly ---
    @Transactional
    ExamResultDTO submitExam(String username, Integer examId, ExamSubmitDTO dto);

    // --- 4. ACADEMIC VAULT: Fetch published exams for the dashboard ---
    @Transactional(readOnly = true)
    VaultExamsResponseDTO getVaultExams(String username, Integer classId);
}
