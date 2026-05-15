package se.ltu.eduo.llm;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

//mock service som tar emot en promt och returnerar ett fördefinerat svar
@Service
@Profile("!ollama")
public class MockLlmService implements LlmService {

    @Override
    public String generateStudyQuestions(String prompt) {
        return "ett dokument med frågor";
    }
}
