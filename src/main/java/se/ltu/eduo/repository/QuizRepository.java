package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.collection.Quiz;

import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {
}