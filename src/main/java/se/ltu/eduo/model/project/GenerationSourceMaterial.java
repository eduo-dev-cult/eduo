package se.ltu.eduo.model.project;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Join table recording which source materials were used in a given generation.
 * Uses a composite primary key rather than a surrogate ID because this entity
 * has no independent identity beyond the relationship it represents.
 */
@Entity
@Table(name = "generation_source_materials", schema = "eduo")
@Getter
@Setter
@NoArgsConstructor
public class GenerationSourceMaterial {

    @EmbeddedId
    private GenerationSourceMaterialId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("generationId")
    @JoinColumn(name = "generation_id", nullable = false, updatable = false)
    private Generation generation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("sourceMaterialId")
    @JoinColumn(name = "source_material_id", nullable = false, updatable = false)
    private SourceMaterial sourceMaterial;

    public GenerationSourceMaterial(Generation generation, SourceMaterial sourceMaterial) {
        this.generation = generation;
        this.sourceMaterial = sourceMaterial;
        this.id = new GenerationSourceMaterialId(generation.getId(), sourceMaterial.getId());
    }

    /**
     * Composite key for {@link GenerationSourceMaterial}. Both fields are
     * immutable after construction — the join record is never updated, only
     * inserted or deleted.
     */
    @Embeddable
    @Getter
    @NoArgsConstructor
    public static class GenerationSourceMaterialId implements Serializable {

        @Column(name = "generation_id", nullable = false, updatable = false)
        private UUID generationId;

        @Column(name = "source_material_id", nullable = false, updatable = false)
        private UUID sourceMaterialId;

        public GenerationSourceMaterialId(UUID generationId, UUID sourceMaterialId) {
            this.generationId = generationId;
            this.sourceMaterialId = sourceMaterialId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GenerationSourceMaterialId other)) return false;
            return generationId.equals(other.generationId)
                    && sourceMaterialId.equals(other.sourceMaterialId);
        }

        @Override
        public int hashCode() {
            return 31 * generationId.hashCode() + sourceMaterialId.hashCode();
        }
    }
}
