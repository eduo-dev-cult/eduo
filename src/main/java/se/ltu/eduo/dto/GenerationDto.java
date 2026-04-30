package se.ltu.eduo.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link se.ltu.eduo.model.project.Generation}
 */
@Value
public class GenerationDto implements Serializable {
    UUID id;
    Instant createdAt;
    Instant updatedAt;
    QuizDto quiz;
}