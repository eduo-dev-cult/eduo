package se.ltu.eduo.collection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.collection.model.SourceMaterial;

import java.util.UUID;

public interface SourceMaterialRepository extends JpaRepository<SourceMaterial, UUID> {
}