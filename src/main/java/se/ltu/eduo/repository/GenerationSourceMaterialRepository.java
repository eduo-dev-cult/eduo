package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.collection.GenerationSourceMaterial;

public interface GenerationSourceMaterialRepository extends JpaRepository<GenerationSourceMaterial, GenerationSourceMaterial.GenerationSourceMaterialId> {
}