package lk.ijse.examsybackend.controller;

import lk.ijse.examsybackend.dto.request.MockExamRequestDTO;
import lk.ijse.examsybackend.entity.MockExam;
import lk.ijse.examsybackend.service.GroqMockExamService;
import lk.ijse.examsybackend.util.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mock-exams")
@RequiredArgsConstructor
public class MockExamController {

    private final GroqMockExamService groqMockExamService;

    @PostMapping("/generate")
    public ResponseEntity<APIResponse<MockExam>> generateExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MockExamRequestDTO request) {

        MockExam exam = groqMockExamService.generateAndSaveExam(
                userDetails.getUsername(),
                request.getSubject(),
                request.getTopic(),
                request.getDifficulty(),
                request.getCount()
        );

        return ResponseEntity.ok(new APIResponse<>(200, "AI Exam Generated successfully", exam));
    }
}