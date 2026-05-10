package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.project.SourceMaterial;

import java.util.UUID;

public interface SourceMaterialRepository extends JpaRepository<SourceMaterial, UUID> {
}