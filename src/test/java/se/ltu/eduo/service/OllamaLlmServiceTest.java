package se.ltu.eduo.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient; // To test against a real Ollama instance, we need to use the same RestClient that the service uses.

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// run only me as test via terminal command:
// ./mvnw -Dtest=OllamaLlmServiceTest test

class OllamaLlmServiceTest {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "gemma:2b";

    private static final String STUDY_QUESTIONS_PROMPT = """
            Reply with exactly five short multiple choice study questions about Agile theory.
            Use this format:
            1. Question
               A) Option
               B) Option
               C) Option
               D) Option
               Answer: X
            """;

    @Test
    /**
     * This test verifies that the OllamaLlmService can successfully connect to the Ollama API and
     * receive a non-blank response when generating study questions. It uses assumptions to skip
     * the test if Ollama is not running, preventing false failures in environments where Ollama
     * is not available.
     */
    void generateStudyQuestions_returnsNonBlankResponseFromOllama() {
        // Can fetch from terminal, otherwise default values.
        String baseUrl = System.getProperty("ollama.base-url", DEFAULT_BASE_URL);
        String model = System.getProperty("ollama.model", DEFAULT_MODEL);

        assumeTrue(isOllamaRunning(baseUrl), "Skipping test because Ollama is not running.");

        // start service to test
        OllamaLlmService service = new OllamaLlmService(RestClient.builder(), baseUrl, model);

        // response  after sending prompt contained herein.
        String response = service.generateStudyQuestions(STUDY_QUESTIONS_PROMPT);

        assertThat(response).isNotBlank();  // If Ollama is running and responding, we should get a non-blank response.
        System.out.println("Ollama response: " + response);
    }

    // Helper method to check if Ollama is running before executing tests that depend on it.
    private boolean isOllamaRunning(String baseUrl) {
        try {
            RestClient.builder()
                    .baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri("/")
                    .retrieve()
                    .toBodilessEntity();

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}