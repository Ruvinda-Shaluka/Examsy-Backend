package lk.ijse.examsybackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.examsybackend.entity.MockExam;
import lk.ijse.examsybackend.entity.MockQuestion;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.repository.MockExamRepo;
import lk.ijse.examsybackend.repository.StudentRepo;
import lk.ijse.examsybackend.service.GroqMockExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqMockExamServiceImpl implements GroqMockExamService {

    private final MockExamRepo mockExamRepository;
    private final StudentRepo studentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    @Transactional
    @Override
    public MockExam generateAndSaveExam(String username, String subject, String topic, String difficulty, int count) {

        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        int maxRetries = 3;
        JsonNode generatedData = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            generatedData = callGroq(subject, topic, difficulty, count);

            if (isValidExam(generatedData, count)) {
                break;
            }

            if (attempt == maxRetries - 1) {
                throw new RuntimeException("AI failed to generate valid exam after retries");
            }
        }

        MockExam exam = MockExam.builder()
                .student(student)
                .subject(subject)
                .topic(topic)
                .difficulty(difficulty)
                .build();

        List<MockQuestion> questionEntities = new ArrayList<>();

        for (JsonNode qNode : generatedData.path("questions")) {
            MockQuestion q = MockQuestion.builder()
                    .mockExam(exam)
                    .questionText(qNode.path("questionText").asText())
                    .optionA(qNode.path("options").get(0).asText())
                    .optionB(qNode.path("options").get(1).asText())
                    .optionC(qNode.path("options").get(2).asText())
                    .optionD(qNode.path("options").get(3).asText())
                    .correctOptionIndex(qNode.path("correctOptionIndex").asInt())
                    .explanation(qNode.path("explanation").asText())
                    .build();

            questionEntities.add(q);
        }

        exam.setQuestions(questionEntities);
        return mockExamRepository.save(exam);
    }

    // =========================
    // 🔥 GROQ CALL WITH BETTER PROMPT
    // =========================
    private JsonNode callGroq(String subject, String topic, String difficulty, int count) {

        long seed = System.currentTimeMillis();

        String prompt = String.format("""
                Generate a mock exam in STRICT JSON format.

                RULES:
                - All answers MUST be 100%% correct.
                - Double-check every calculation.
                - Do NOT guess answers.
                - Ensure explanation matches the correct answer.
                - If math is involved, recompute carefully.
                - Do NOT produce invalid or inconsistent answers.

                Self-check before returning:
                1. Verify correct option is truly correct
                2. Verify explanation matches answer
                3. Fix any errors before output

                Subject: %s
                Topic: %s
                Difficulty: %s
                Number of Questions: %d
                Seed: %d

                Output ONLY valid JSON:
                {
                  "questions": [
                    {
                      "questionText": "...",
                      "options": ["A", "B", "C", "D"],
                      "correctOptionIndex": 0,
                      "explanation": "..."
                    }
                  ]
                }
                """, subject, topic, difficulty, count, seed);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.2, // slight randomness improves reasoning
                "response_format", Map.of("type", "json_object")
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(GROQ_URL, request, String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            return objectMapper.readTree(content);

        } catch (Exception e) {
            throw new RuntimeException("Groq API error: " + e.getMessage());
        }
    }

    // =========================
    // 🧠 VALIDATION LAYER
    // =========================
    private boolean isValidExam(JsonNode data, int expectedCount) {

        if (!data.has("questions")) return false;

        JsonNode questions = data.get("questions");

        if (!questions.isArray() || questions.size() != expectedCount) return false;

        for (JsonNode q : questions) {

            // basic structure check
            if (!q.has("questionText") ||
                    !q.has("options") ||
                    !q.has("correctOptionIndex") ||
                    !q.has("explanation")) {
                return false;
            }

            // options must be 4
            if (q.get("options").size() != 4) return false;

            int correctIndex = q.get("correctOptionIndex").asInt();

            if (correctIndex < 0 || correctIndex > 3) return false;

            // 🧪 simple math validation heuristic
            String questionText = q.get("questionText").asText().toLowerCase();
            String explanation = q.get("explanation").asText();

            if (questionText.contains("grams") || questionText.contains("calculate")) {
                if (explanation.length() < 10) return false; // weak explanation
            }
        }

        return true;
    }
}