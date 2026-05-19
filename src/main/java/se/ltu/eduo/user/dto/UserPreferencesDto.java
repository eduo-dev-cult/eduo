package se.ltu.eduo.user.dto;

import se.ltu.eduo.collection.model.GenerationFocusArea;
import se.ltu.eduo.collection.model.GenerationLanguage;

public class UserPreferencesDto {
    private int numOfQuestions;
    private GenerationLanguage language;
    private GenerationFocusArea focusArea;
    private String topics;

    private boolean defaultEasy;
    private boolean defaultMedium;
    private boolean defaultHard;

    private boolean defaultMultipleChoice;
    private boolean defaultOpenEnded;
    private boolean defaultTrueFalse;

    private boolean defaultCorrectAnswers;
    private boolean defaultExplanations;
    private boolean defaultDescription;
}
