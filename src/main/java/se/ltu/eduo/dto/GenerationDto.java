package se.ltu.eduo.dto;

import lombok.Value;
import se.ltu.eduo.model.collection.Generation;
import se.ltu.eduo.model.collection.GenerationFocusArea;
import se.ltu.eduo.model.collection.GenerationLanguage;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link Generation}
 */
@Value
public class GenerationDto implements Serializable {
    UUID id;
    CollectionDto collection;
    Instant createdAt;
    Instant updatedAt;
    QuizDto quiz;
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
}