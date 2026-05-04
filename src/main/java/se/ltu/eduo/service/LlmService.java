package se.ltu.eduo.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

// interface för interaktion med ai, används för både mock och riktig ai implementation
@Service
@Profile("ollama")
public interface LlmService {
    String generateStudyQuestions(String prompt);
}
