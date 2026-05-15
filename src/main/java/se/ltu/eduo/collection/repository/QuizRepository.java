package se.ltu.eduo.collection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.collection.model.Quiz;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
}