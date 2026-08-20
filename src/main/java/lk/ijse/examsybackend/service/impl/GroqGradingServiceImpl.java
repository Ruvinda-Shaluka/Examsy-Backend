package lk.ijse.examsybackend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.ijse.examsybackend.service.GroqGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GroqGradingServiceImpl implements GroqGradingService {

    // Keep the key secure in application.properties
    @Value("${groq.api.key}")
    private String groqApiKey;
    private final String groqApiUrl = "https://api.groq.com/openai/v1/chat/completions";
    private final ObjectMapper objectMapper;

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
                      "model": "llama-3.1-8b-instant",
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
                    .uri(URI.create(groqApiUrl))
                    .header("Authorization", "Bearer " + groqApiKey)
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
            e.printStackTrace(); // Added this to print the full stack trace just in case!
            return fallbackResponse(e.getMessage());
        }
    }

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