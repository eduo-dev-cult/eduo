package se.ltu.eduo.service;

import org.springframework.context.annotation.Profile; // För skilja på mock och riktiga.
import org.springframework.stereotype.Service; // För att markera denna klass som en service: spring ser det som en bean.
import org.springframework.web.client.RestClient; // För att göra http anrop till ollama.
import org.springframework.beans.factory.annotation.Value; // För att hämta konfiguration från application.properties.
import org.springframework.http.MediaType; // För att specificera att vi skickar json i våra anrop.

// interface för interaktion med ai, används för både mock och riktig ai implementation
@Service
@Profile("ollama")
public class OllamaLlmService implements LlmService {

    private final RestClient restClient; // Håller Spring klienten för http anrop.
    private final String model; // Håller vilken modell vi använder, så att vi inte behöver hardkoda det i metoderna.

    // Konstruktor, hämtar från application.properties.
    public OllamaLlmService(
            RestClient.Builder builder,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.model = model;
    }

    @Override
    public String generateStudyQuestions(String prompt) {
        OllamaRequest request = new OllamaRequest(model, prompt, false);

        OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);

        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new IllegalStateException("Ollama returned an empty response");
        }

        return response.response();
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {}
    private record OllamaResponse(String response) {}
}
