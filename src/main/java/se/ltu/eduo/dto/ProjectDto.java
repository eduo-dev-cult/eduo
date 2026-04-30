package se.ltu.eduo.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link se.ltu.eduo.model.project.Project}
 */
@Value
public class ProjectDto implements Serializable {
    UUID id;
    Integer userIdId;
    String name;
    Instant createdAt;
    Instant updatedAt;
    List<SourceMaterialDto> sourceMaterials;
    List<GenerationDto> generations;

    /**
     * DTO for {@link se.ltu.eduo.model.project.SourceMaterial}
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