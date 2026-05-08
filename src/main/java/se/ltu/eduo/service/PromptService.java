package se.ltu.eduo.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {
    public String buildPrompt(String fileString) {

        return """
               Kursmaterial:
               %s
             
               Instruktioner: llminstruktioner som vi skriver själva
               
               """
               .formatted(fileString);
    }
}
