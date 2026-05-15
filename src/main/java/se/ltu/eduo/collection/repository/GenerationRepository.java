package se.ltu.eduo.collection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.collection.model.Generation;

import java.util.UUID;

public interface GenerationRepository extends JpaRepository<Generation, UUID> {
}