package se.ltu.eduo.dto;

import lombok.Value;
import se.ltu.eduo.model.collection.Collection;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Collection}
 */
@Value
public class CollectionDto implements Serializable {
    UUID id;
    Integer ownerId;
    String name;
    Instant createdAt;
    Instant updatedAt;
    List<SourceMaterialDto> sourceMaterials;
    List<GenerationDto> generations;

    /**
     * DTO for {@link se.ltu.eduo.model.collection.SourceMaterial}
     */
    @Value
    public static class SourceMaterialDto implements Serializable {
        UUID id;
        String filename;
        String fileType;
        long fileSizeBytes;
        Instant uploadedAt;
    }
}