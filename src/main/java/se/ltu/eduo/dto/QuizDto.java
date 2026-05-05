package se.ltu.eduo.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link se.ltu.eduo.model.project.Quiz}
 */
@Value
public class QuizDto implements Serializable {
    UUID quizId;
    String name;
    String rawContent;
    Instant createdAt;
    Instant updatedAt;
}