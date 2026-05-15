package se.ltu.eduo.collection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.collection.model.GenerationSourceMaterial;

public interface GenerationSourceMaterialRepository extends JpaRepository<GenerationSourceMaterial, GenerationSourceMaterial.GenerationSourceMaterialId> {
}