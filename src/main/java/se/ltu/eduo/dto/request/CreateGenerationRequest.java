package se.ltu.eduo.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import se.ltu.eduo.model.collection.GenerationFocusArea;
import se.ltu.eduo.model.collection.GenerationLanguage;
import java.io.Serializable;
import java.util.UUID;

public record CreateGenerationRequest(
    // relations
    //UUID collection, unneeded, exists in pathvar
    @NotEmpty
    UUID[] sourceMaterials,
    // endregion relations

    // region settings
    int numOfQuestions,
    GenerationLanguage language,
    GenerationFocusArea focusArea,
    String topics,
    boolean easy,
    boolean medium,
    boolean hard,
    boolean multipleChoice,
    boolean openEnded,
    boolean trueFalse,
    boolean questions,
    boolean correctAnswers,
    boolean explanations,
    boolean description
    // endregion
) {}