package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.collection.Generation;

import java.util.UUID;

public interface GenerationRepository extends JpaRepository<Generation, UUID> {
}