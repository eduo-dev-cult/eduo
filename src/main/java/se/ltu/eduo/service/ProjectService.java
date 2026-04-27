package se.ltu.eduo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.project.*;
import se.ltu.eduo.repository.*;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates creation, retrieval, and deletion of projects and their
 * associated domain objects. All methods that write to the database are
 * transactional. Read methods are marked read-only as a performance hint
 * to the persistence provider.
 *
 * <p>Callers receive the saved or created entity.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SourceMaterialRepository sourceMaterialRepository;
    private final GenerationRepository generationRepository;
    private final GenerationSourceMaterialRepository generationSourceMaterialRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    //region Projects
    // Standard CRUD behaviours.
    // -------------------------------------------------------------------------

    /**
     * Creates and persists a new project owned by the given user.
     * Time of creation is that of the invocation.
     * @param userId the owner of the project
     * @param name the name of the project
     * @return the created project
     */
    @Transactional
    public Project createProject(Integer userId, String name) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found by that ID: " + userId));
        Project project = new Project(user, name);
        return projectRepository.save(project);
    }

    /**
     * Returns a project by ID, throwing if it does not exist.
     */
    @Transactional(readOnly = true)
    public Project getProject(UUID projectId) {
        return projectRepository.findById(projectId)
                                .orElseThrow(() -> new EntityNotFoundException("Project not found with this ID: " + projectId));
    }

    /**
     * Updates the name of an existing project and returns the saved state.
     * {@code updatedAt} is managed by {@link org.hibernate.annotations.UpdateTimestamp}
     * and refreshes automatically on save.
     */
    @Transactional
    public Project updateProject(UUID projectId, String name) {
        Project project = projectRepository.findById(projectId)
                                           .orElseThrow(() -> new EntityNotFoundException("Project not found with this ID: " + projectId));
        project.setName(name);
        return projectRepository.save(project);
    }

    /**
     * Deletes a project and all of its associated source materials, generations,
     * and quizzes. Cascade rules on the entity handle child deletion — this
     * method is the single authorised entry point for project deletion so that
     * any future pre-deletion checks (e.g. ownership verification) have one
     * place to live.
     */
    @Transactional
    public void deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                                           .orElseThrow(() -> new EntityNotFoundException("Project not found with this ID: " + projectId));
        projectRepository.delete(project);
    }

    //endregion projects

    // -------------------------------------------------------------------------
    //region Source material
    // Almost standard - no update as delete and reupload fits that pattern.
    // -------------------------------------------------------------------------

    /**
     * Uploads a file and associates it with the given project.
     */
    @Transactional
    public SourceMaterial createSourceMaterial(UUID projectId,
                                               String filename,
                                               String fileType,
                                               byte[] fileData) {
        Project project = projectRepository.findById(projectId)
                                           .orElseThrow(() -> new EntityNotFoundException("Project not found with this ID: " + projectId));
        SourceMaterial material = new SourceMaterial(project, filename, fileType, fileData);
        return sourceMaterialRepository.save(material);
    }

    /**
     * Returns a source material by ID, throwing if it does not exist.
     */
    @Transactional(readOnly = true)
    public SourceMaterial getSourceMaterial(UUID sourceMaterialId) {
        return sourceMaterialRepository.findById(sourceMaterialId)
                                       .orElseThrow(() -> new EntityNotFoundException("Source material not found with this ID: " + sourceMaterialId));
    }

    /**
     * Delete a source material by supplying its ID. Completes silently if the ID does not exist.
     */
    @Transactional
    public void deleteSourceMaterial(UUID sourceMaterialId)
    {
        //TODO throw if missing?
        sourceMaterialRepository.deleteById(sourceMaterialId);
    }
    //endregion Source material

    // -------------------------------------------------------------------------
    //region Generation
    // -------------------------------------------------------------------------

    /**
     * Creates a generation attempt for the given project, recording which
     * source materials were selected for this run. Loads all materials in a
     * single query via {@code findAllById} to avoid N+1 lookups.
     *
     * <p>The caller is responsible for subsequently calling
     * {@link #createQuiz(UUID, String, String)} once the AI response is available.
     */
    @Transactional
    public Generation createGeneration(UUID projectId, List<UUID> sourceMaterialIds) {
        Project project = projectRepository.findById(projectId)
                                           .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        Generation generation = generationRepository.save(new Generation(project));

        List<SourceMaterial> materials = sourceMaterialRepository.findAllById(sourceMaterialIds);
        List<GenerationSourceMaterial> joins = materials.stream()
                                                        .map(material -> new GenerationSourceMaterial(generation, material))
                                                        .toList();
        generationSourceMaterialRepository.saveAll(joins);

        return generation;
    }

    /**
     * Returns a generation by ID, throwing if it does not exist.
     */
    @Transactional(readOnly = true)
    public Generation getGeneration(UUID generationId) {
        return generationRepository.findById(generationId)
                                   .orElseThrow(() -> new EntityNotFoundException("Generation not found: " + generationId));
    }

    /**
     * Delete a Generation by supplying its ID. Completes silently if the ID does not exist.
     */
    @Transactional
    public void deleteGeneration(UUID generationId)
    {
        //TODO throw if missing?
        generationRepository.deleteById(generationId);
    }
    //endregion Generation

    // -------------------------------------------------------------------------
    //region Quiz
    // -------------------------------------------------------------------------

    /**
     * Persists the raw AI output as a quiz attached to the given generation.
     * Expected to be called immediately after the AI provider returns a response.
     */
    @Transactional
    public Quiz createQuiz(UUID generationId, String name, String rawContent) {
        Generation generation = generationRepository.findById(generationId)
                                                    .orElseThrow(() -> new EntityNotFoundException("Generation not found: " + generationId));
        Quiz quiz = new Quiz(generation, name, rawContent);
        return quizRepository.save(quiz);
    }


    /**
     * Returns a quiz by ID, throwing if it does not exist.
     */
    @Transactional(readOnly = true)
    public Quiz getQuiz(UUID quizId) {
        return quizRepository.findById(quizId)
                             .orElseThrow(() -> new EntityNotFoundException("Quiz not found: " + quizId));
    }

    /**
     * Updates the name and raw content of an existing quiz and returns the saved state.
     * {@code updatedAt} is managed by {@link org.hibernate.annotations.UpdateTimestamp}
     * and refreshes automatically on save.
     */
    @Transactional
    public Quiz updateQuiz(UUID quizId, String name, String rawContent) {
        Quiz quiz = quizRepository.findById(quizId)
                                  .orElseThrow(() -> new EntityNotFoundException("Quiz not found: " + quizId));
        quiz.setName(name);
        quiz.setRawContent(rawContent);
        return quizRepository.save(quiz);
    }

    /**
     * Delete a quiz by its ID. Completes silently if the ID does not exist.
     */
    @Transactional
    public void deleteQuiz(UUID quizId)
    {
        quizRepository.deleteById(quizId);
    }
    //endregion Quiz
}
