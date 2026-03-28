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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqMockExamServiceImpl implements GroqMockExamService {

    @Value("${groq.api.url}")
    private static String GROQ_URL;
    private final MockExamRepo mockExamRepository;
    private final StudentRepo studentRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    @Value("${groq.api.key}")
    private String groqApiKey;
    @Value("${groq.api.key1}")
    private String groqApiKey1;
    @Value("${groq.api.key2}")
    private String groqApiKey2;


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

    @Override
    public JsonNode gradeShortAnswer(String questionText, String modelAnswer, String studentAnswer, java.math.BigDecimal maxPoints) {
        String prompt = String.format("""
                You are a highly strict, fair, and accurate exam grader.
                
                TASK:
                Evaluate the student's answer based ONLY on the teacher's model answer.
                
                INPUT:
                Question: %s
                Model Answer: %s
                Student Answer: %s
                Maximum Score: %s
                
                GRADING INSTRUCTIONS:
                1. Identify key points in the model answer.
                2. Compare the student answer against those key points.
                3. Award marks proportionally based on correct coverage.
                4. Do NOT give full marks unless all key points are covered correctly.
                5. Ignore minor grammar mistakes unless they affect meaning.
                6. Penalize missing, incorrect, or irrelevant information.
                7. Do NOT guess — base grading strictly on provided answers.
                
                SCORING RULES:
                - Score must be between 0 and %s
                - Score must be logical and consistent with explanation
                - Use decimal scoring if needed (e.g., 2.5)
                
                SELF-CHECK (VERY IMPORTANT):
                Before giving final output:
                - Ensure score matches the explanation
                - Ensure no contradiction between feedback and score
                - Ensure score is not higher than justified
                
                OUTPUT FORMAT (STRICT JSON ONLY):
                {
                  "awarded_score": number,
                  "feedback": "Clear, concise explanation mentioning correct points and missing points"
                }
                """, questionText, modelAnswer, studentAnswer, maxPoints.toString(), maxPoints);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey1);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.1, // Very low temperature for strict, analytical grading
                "response_format", Map.of("type", "json_object")
        );

        org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject("https://api.groq.com/openai/v1/chat/completions", request, String.class);
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new RuntimeException("AI Grading failed: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> evaluateAnswer(String questionText, String modelAnswer, String studentOcrText) {

        String systemPrompt = """
                You are an expert, strict but fair exam grader.
                
                You are grading answers extracted from handwritten exam papers using OCR.
                The OCR text may contain:
                - Spelling mistakes (e.g., "rnass" instead of "mass")
                - Missing punctuation
                - Broken or merged words
                - Random noise characters
                
                You MUST intelligently interpret the student's intent.
                
                GRADING RULES:
                1. Compare the student's answer with the model answer concept-by-concept.
                2. Identify key points from the model answer.
                3. Check which points are:
                   - Fully correct
                   - Partially correct
                   - Missing
                   - Incorrect
                4. Be strict but fair:
                   - Award partial marks where appropriate
                   - Do NOT give full marks unless the answer is conceptually complete
                5. Ignore minor OCR-related spelling mistakes if meaning is clear.
                
                OUTPUT REQUIREMENTS:
                - Return ONLY valid JSON (no markdown, no explanation outside JSON)
                - Follow this EXACT structure:
                {
                  "suggestedScore": <integer 0 to 100>,
                  "matchedConcepts": ["..."],
                  "missingConcepts": ["..."],
                  "incorrectParts": ["..."],
                  "comments": "<clear, short feedback>",
                  "confidence": "<High | Medium | Low>"
                }
                
                SCORING:
                - Base score on percentage of correct concepts covered
                - Penalize missing key points heavily
                - Penalize incorrect explanations moderately
                - Reward clarity and completeness
                
                IMPORTANT:
                Do NOT hallucinate content.
                Do NOT include anything outside the JSON.
                """;

        String userPrompt = String.format("""
                Question:
                %s
                
                Model Answer:
                %s
                
                Student Answer (OCR Extracted):
                %s
                """, questionText, modelAnswer, studentOcrText);

        try {
            String requestBody = """
                    {
                      "model": "llama3-70b-8192",
                      "messages": [
                        { "role": "system", "content": %s },
                        { "role": "user", "content": %s }
                      ],
                      "temperature": 0.1, 
                      "response_format": { "type": "json_object" }
                    }
                    """.formatted(
                    objectMapper.writeValueAsString(systemPrompt),
                    objectMapper.writeValueAsString(userPrompt)
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Authorization", "Bearer " + groqApiKey2)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                String aiResponseText = rootNode.path("choices").get(0).path("message").path("content").asText();
                return objectMapper.readValue(aiResponseText, Map.class);
            } else {
                System.err.println("Groq API Error: " + response.body());
                return fallbackResponse("API Error: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("Failed to communicate with Groq: " + e.getMessage());
            return fallbackResponse("Exception occurred during AI grading.");
        }
    }

    // fallback to match your new JSON schema arrays perfectly
    private Map<String, Object> fallbackResponse(String errorDetails) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("suggestedScore", 0);
        fallback.put("matchedConcepts", List.of());
        fallback.put("missingConcepts", List.of());
        fallback.put("incorrectParts", List.of("System Error: Could not connect to AI."));
        fallback.put("comments", "Manual review required. Details: " + errorDetails);
        fallback.put("confidence", "Low");
        return fallback;
    }
}