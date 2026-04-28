package se.ltu.eduo.service;

import org.springframework.stereotype.Service;

//mock service som tar emot en promt och returnerar ett fördefinerat svar
@Service
public class MockLlmService implements LlmService {

    @Override
    public String generateStudyQuestions(String prompt) {
        return "ett dokument med frågor";
    }
}
