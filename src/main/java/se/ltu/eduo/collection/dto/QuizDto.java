package se.ltu.eduo.collection.dto;

import lombok.Value;
import se.ltu.eduo.collection.model.Quiz;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link Quiz}
 */
@Value
public class QuizDto implements Serializable {
    UUID id;
    String name;
    String rawContent;
    Instant createdAt;
    Instant updatedAt;
}