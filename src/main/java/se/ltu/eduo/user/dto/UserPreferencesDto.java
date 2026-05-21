package se.ltu.eduo.user.dto;

import lombok.Data;
import se.ltu.eduo.collection.model.GenerationFocusArea;
import se.ltu.eduo.collection.model.GenerationLanguage;

@Data
public class UserPreferencesDto {
    private int numOfQuestions;
    private GenerationLanguage language;
    private GenerationFocusArea focusArea;
    private String topics;

    private boolean easy;
    private boolean medium;
    private boolean hard;

    private boolean multipleChoice;
    private boolean openEnded;
    private boolean trueFalse;

    private boolean correctAnswers;
    private boolean explanations;
    private boolean description;
}
