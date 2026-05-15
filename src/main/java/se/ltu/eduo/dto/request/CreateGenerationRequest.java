package se.ltu.eduo.dto.request;


import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import se.ltu.eduo.model.collection.GenerationFocusArea;
import se.ltu.eduo.model.collection.GenerationLanguage;
import java.util.UUID;

public record CreateGenerationRequest(
    // relations
    //UUID collection, unneeded, exists in pathvar
    @NotEmpty(message = "Must have at least one source material")
    UUID[] sourceMaterials,
    // endregion relations

    // region settings
    @Min(1)
    @Max(50)
    int numOfQuestions,
    GenerationLanguage language,
    GenerationFocusArea focusArea,
    String topics, //allow empty string?

    //difficulty
    boolean easy,
    boolean medium,
    boolean hard,

    //questiontype
    boolean multipleChoice,
    boolean openEnded,
    boolean trueFalse,

    //must always generate questions
    @AssertTrue
    boolean questions, //fixme carries no information if always true


    //should there be a quiz description, answer explanations or quiz description (all optional)
    Boolean correctAnswers,
    Boolean explanations,
    Boolean description
    // endregion
) {
    @AssertTrue(message = "At least one difficulty option must be selected")
    public boolean isAtLeastOneDifficultySelected() {
        return easy || medium || hard;
    }

    @AssertTrue(message = "At least one question type must be selected")
    public boolean isAtLeastOneQuestionTypeSelected() {
        return openEnded || trueFalse || multipleChoice;
    }

    @AssertTrue(message = "Topics must be specified if they are the focus area")
    public boolean isTopicsSpecifiedWhenFocusAreaIsTopics() {
        if(focusArea != GenerationFocusArea.TOPICS)
        {
            return true;
        }else return topics != null && !topics.isEmpty();
    }

    public CreateGenerationRequest {
        correctAnswers = correctAnswers != null ? correctAnswers : false;
        explanations = explanations != null ? explanations : false;
        description = description != null ? description : false;
    }
}