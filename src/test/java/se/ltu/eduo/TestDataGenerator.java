package se.ltu.eduo;

import se.ltu.eduo.dto.request.CreateGenerationRequest;
import se.ltu.eduo.model.collection.GenerationFocusArea;
import se.ltu.eduo.model.collection.GenerationLanguage;
import se.ltu.eduo.service.AuthService;

import java.util.UUID;

public class TestDataGenerator {

    public static Integer persistUser(AuthService authService) {
        return authService.createUser("Test", "User", "tuser", "pass").getId();
    }

    // ---------------------------------------------------------------
    // CreateGenerationRequest factories
    // ---------------------------------------------------------------

    public static CreateGenerationRequest validGenerationRequest(UUID... sourceMaterialIds) {
        return new CreateGenerationRequest(
                sourceMaterialIds,
                10,
                GenerationLanguage.ENGLISH,
                GenerationFocusArea.ENTIRE_MATERIAL,
                "",
                true, true, false,
                false, false, true,
                true,
                false, false, false
        );
    }

    /** Fails {@code isAtLeastOneDifficultySelected} — all difficulty flags are false. */
    public static CreateGenerationRequest invalidRequest_noDifficultySelected(UUID... sourceMaterialIds) {
        return new CreateGenerationRequest(
                sourceMaterialIds,
                10,
                GenerationLanguage.ENGLISH,
                GenerationFocusArea.ENTIRE_MATERIAL,
                "",
                false, false, false,
                false, false, true,
                true,
                false, false, false
        );
    }

    /** Fails {@code isAtLeastOneQuestionTypeSelected} — all question type flags are false. */
    public static CreateGenerationRequest invalidRequest_noQuestionTypeSelected(UUID... sourceMaterialIds) {
        return new CreateGenerationRequest(
                sourceMaterialIds,
                10,
                GenerationLanguage.ENGLISH,
                GenerationFocusArea.ENTIRE_MATERIAL,
                "",
                true, true, false,
                false, false, false,
                false,
                false, false, false
        );
    }
}