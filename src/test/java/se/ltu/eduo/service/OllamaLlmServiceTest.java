package se.ltu.eduo.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient; // To test against a real Ollama instance, we need to use the same RestClient that the service uses.

// For reading config file to get cirrently active set model.
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// run only me as test via terminal command:
// ./mvnw -Dtest=OllamaLlmServiceTest test

class OllamaLlmServiceTest {

    private static final boolean in_swedish = true; // Set to true to print prompts and responses in Swedish, false for English. Only for demonstration.
    private static final String OLLAMA_CONFIG_RESOURCE = "application-ollama.properties";
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "gemma:2b";
    

    private static final String STUDY_QUESTIONS_PROMPT = in_swedish ? """
            Svara med exakt fem korta flervalsfrågor om agil teori.
            Använd detta format:
            1. Fråga
               A) Alternativ
               B) Alternativ
               C) Alternativ
               D) Alternativ
               Svar: X
            """ : """
            Reply with exactly five short multiple choice study questions about Agile theory.
            Use this format:
            1. Question
               A) Option
               B) Option
               C) Option
               D) Option
               Answer: X
            """;


    private static final String MODEL_QUESTION_PROMPT = in_swedish ? """
            Svara med exakt information om modellen du kör, inklusive namn, version och eventuella relevanta detaljer om dess kapabiliteter eller begränsningar. Formatera ditt svar enligt följande:
                Modellnamn: [ditt modellnamn]
                Version: [din modellversion]
                Kapabiliteter: [kort beskrivning av vad din modell kan göra bra]
                Begränsningar: [kort beskrivning av eventuella kända begränsningar eller svagheter hos din modell]
            """ : """
            Reply with exact information about the model you are running on, including name, version, and any relevant details about its capabilities or limitations. Format your response as follows:
                Model Name: [your model name]
                Version: [your model version]
                Capabilities: [brief description of what your model can do well]
                Limitations: [brief description of any known limitations or weaknesses of your model]
            """;
    @Test
    /**
     * This test verifies that the OllamaLlmService can successfully connect to the Ollama API and
     * receive a non-blank response when generating study questions. It uses assumptions to skip
     * the test if Ollama is not running, preventing false failures in environments where Ollama
     * is not available.
     */
    void generateStudyQuestions_returnsNonBlankResponseFromOllama() {
        // Load configuration for Ollama from application-ollama.properties, with defaults if not found.
        Properties ollamaConfig = loadOllamaConfig();
        String baseUrl = ollamaConfig.getProperty("ollama.base-url", DEFAULT_BASE_URL);
        String model = ollamaConfig.getProperty("ollama.model", DEFAULT_MODEL);

        assumeTrue(isOllamaRunning(baseUrl), "Skipping test because Ollama is not running.");

        System.out.println("Testing Ollama model: " + model);

        // start service to test
        OllamaLlmService service = new OllamaLlmService(RestClient.builder(), baseUrl, model);

        // First we check if we can get model information, which also serves as a basic connectivity test.
        String modelInfoResponse = service.generateStudyQuestions(MODEL_QUESTION_PROMPT);
        assertThat(modelInfoResponse).isNotBlank(); // If Ollama is running and responding, we should get a non-blank response.

        // response  after sending prompt contained herein.
        String response = service.generateStudyQuestions(STUDY_QUESTIONS_PROMPT);

        assertThat(response).isNotBlank();  // If Ollama is running and responding, we should get a non-blank response.
        System.out.println("Ollama LLM model info as per model response: \n\n" + modelInfoResponse + "\n\n");
        System.out.println("Ollama MCQ generation prompt response: \n\n" + response);
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

    private Properties loadOllamaConfig() {
        Properties properties = new Properties();

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(OLLAMA_CONFIG_RESOURCE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + OLLAMA_CONFIG_RESOURCE, e);
        }

        return properties;
    }
}