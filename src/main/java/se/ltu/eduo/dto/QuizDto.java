package se.ltu.eduo.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link se.ltu.eduo.model.collection.Quiz}
 */
@Value
public class QuizDto implements Serializable {
    UUID id;
    String name;
    String rawContent;
    Instant createdAt;
    Instant updatedAt;
}