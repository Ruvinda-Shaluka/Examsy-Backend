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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final ObjectMapper objectMapper = new ObjectMapper(); // Fixed the ObjectMapper issue!
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Transactional
    @Override
    public MockExam generateAndSaveExam(String username, String subject, String topic, String difficulty, int count) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // 🟢 FIXED: Injected a cache-busting random seed to guarantee completely unique questions!
        long randomSeed = System.currentTimeMillis();

        String prompt = String.format("""
            Generate a completely unique and highly randomized mock exam paper in strictly valid JSON format.
            To ensure variety, use this randomization seed: %d
            
            Subject: %s
            Topic: %s
            Difficulty: %s
            Number of Questions: %d

            Do not repeat standard textbook questions. Be creative. 
            Format the JSON exactly like this, with no markdown formatting or extra text:
            {
              "questions": [
                {
                  "questionText": "Question here?",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOptionIndex": 0,
                  "explanation": "Short explanation"
                }
              ]
            }
            """, randomSeed, subject, topic, difficulty, count);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.9 // 🟢 Slightly increased temperature for more creative variety
        );

        String groqUrl = "https://api.groq.com/openai/v1/chat/completions";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            String responseJson = restTemplate.postForObject(groqUrl, request, String.class);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String contentString = rootNode.path("choices").get(0).path("message").path("content").asText();
            JsonNode generatedData = objectMapper.readTree(contentString);

            MockExam exam = MockExam.builder()
                    .student(student)
                    .subject(subject)
                    .topic(topic)
                    .difficulty(difficulty)
                    .build();

            List<MockQuestion> questionEntities = new ArrayList<>();
            JsonNode questionsArray = generatedData.path("questions");

            for (JsonNode qNode : questionsArray) {
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

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate exam from AI: " + e.getMessage());
        }
    }
}