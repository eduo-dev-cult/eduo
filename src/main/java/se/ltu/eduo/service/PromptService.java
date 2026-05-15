package se.ltu.eduo.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {
    public String buildPrompt(String fileString, String settings) {

        return """
                Course material:
                %s
                
                System:
                Generate a university level educational quiz based on the instructions below.
                
                
                User settings:
                %s
                
                
                """
                .formatted(fileString,
                        settings);

    }


}
