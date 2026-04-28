package se.ltu.eduo.service;

// interface för interaktion med ai, används för både mock och riktig ai implementation
public interface LlmService {
    String generateStudyQuestions(String prompt);
}
