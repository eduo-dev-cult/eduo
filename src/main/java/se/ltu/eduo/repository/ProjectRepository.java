package se.ltu.eduo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.ltu.eduo.model.project.Project;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
}