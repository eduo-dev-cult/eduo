package se.ltu.eduo.dto.request;


import se.ltu.eduo.model.collection.GenerationFocusArea;
import se.ltu.eduo.model.collection.GenerationLanguage;
import java.io.Serializable;
import java.util.UUID;

public class CreateGenerationRequest implements Serializable {
    //relations
    UUID collection; //id of collection it belongs to
    UUID[] sourceMaterials;
    //endregion relations

    //region settings
    int numOfQuestions;
    GenerationLanguage language;
    GenerationFocusArea focusArea;
    String topics;
    boolean easy;
    boolean medium;
    boolean hard;
    boolean multipleChoice;
    boolean openEnded;
    boolean trueFalse;
    boolean questions;
    boolean correctAnswers;
    boolean explanations;
    boolean description;
    //endregion
}