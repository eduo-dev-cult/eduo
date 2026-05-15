package se.ltu.eduo.collection.dto;

import lombok.Getter;
import lombok.Setter;
import se.ltu.eduo.collection.model.Generation;
import se.ltu.eduo.collection.model.GenerationFocusArea;
import se.ltu.eduo.collection.model.GenerationLanguage;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Generation}
 */
@Setter
@Getter
public class GenerationDto implements Serializable {
    //metadata & id
    UUID id;
    Instant createdAt;
    Instant updatedAt;

    //relations
    UUID collectionId;
    QuizDto quiz; //the quiz that was generated should be contained due to 1:1
    List<CollectionDto.SourceMaterialDto> sourceMaterials; //materials used list (metadata only)
    //settings
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