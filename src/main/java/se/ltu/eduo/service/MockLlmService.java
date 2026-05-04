package se.ltu.eduo.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

//mock service som tar emot en promt och returnerar ett fördefinerat svar
@Service
@Profile("mock") // Så att vi kan deaktivera denna profil. Config i:
//                  src/main/resources/application.properties
public class MockLlmService implements LlmService {

    @Override
    public String generateStudyQuestions(String prompt) {
        return "ett dokument med frågor";
    }
}
