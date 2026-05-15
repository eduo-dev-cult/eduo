package se.ltu.eduo.service;

import org.springframework.stereotype.Service;
import se.ltu.eduo.dto.request.CreateGenerationRequest;
import java.util.ArrayList;
import java.util.List;


@Service
public class SettingsService {

    public String allSettings(CreateGenerationRequest request) {

        List<String> settings = new ArrayList<>();

        settings.add("NUMBER OF QUESTIONS:");
        settings.add(String.valueOf(request.numOfQuestions()));

        settings.add("LANGUAGE:");
        settings.add("Language: " + request.language().language());

        settings.add("FOCUS AREA:");
        settings.add("Focus area =  " +
                request.focusArea().format(request.topics()));

        settings.add("DIFFICULTY:");
        settings.addAll(difficultySettings(request));

        settings.add("QUESTION TYPES:");
        settings.addAll(questionTypeSettings(request));

        settings.add("OUTPUT RULES:");
        settings.addAll(llmRules(request));

        return String.join("\n", settings);
    }


    private List<String> llmRules(CreateGenerationRequest request){
        List<String> rules = new ArrayList<>();

        if (request.correctAnswers()){
            rules.add ("Include multiple correct answers per question");
        }

        if (request.explanations()){
            rules.add ("Include explanations for all answers");
        }

        if (request.description()){
            rules.add("Include a short description before each quiz");
        }

        return rules;
    }


    private List<String> difficultySettings(CreateGenerationRequest request) {

        List<String> selected = new ArrayList<>();

        if (request.easy()) selected.add("easy");
        if (request.medium()) selected.add("medium");
        if (request.hard()) selected.add("hard");

        List<String> rules = new ArrayList<>();

        if (selected.size() == 1) {
            rules.add(selected.getFirst());
        } else {
            rules.add("Mix difficulty levels (" + String.join(", ", selected) + ").");
            rules.add("Distribute questions evenly across the selected difficulty levels.");
        }

        return rules;
    }

    private List<String> questionTypeSettings(CreateGenerationRequest request) {

        List<String> selected = new ArrayList<>();

        if (request.multipleChoice()) selected.add("multiple choice");
        if (request.openEnded()) selected.add("open ended");
        if (request.trueFalse()) selected.add("true/false");

        List<String> rules = new ArrayList<>();

        if (selected.size() == 1) {
            rules.add("Use only " + selected.getFirst() + " questions.");
        } else {
            rules.add("Mix question types (" + String.join(", ", selected) + ").");
            rules.add("Distribute selected question types equally.");
        }

        return rules;
    }

}
