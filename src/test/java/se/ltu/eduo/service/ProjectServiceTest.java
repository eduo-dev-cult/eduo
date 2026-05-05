package se.ltu.eduo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.TestContainersInitializer;
import se.ltu.eduo.model.project.*;
import se.ltu.eduo.repository.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests ProjectService behaviour end-to-end against a real PostgreSQL instance
 * managed by Testcontainers.
 *
 * Uses @SpringBootTest rather than @DataJpaTest because ProjectService is a
 * Spring-managed bean that requires the full application context to be wired.
 *
 * Each test is @Transactional so that database changes are rolled back
 * automatically after each test, keeping tests independent without manual cleanup.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ProjectServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SourceMaterialRepository sourceMaterialRepository;

    @Autowired
    private GenerationRepository generationRepository;

    @Autowired
    private GenerationSourceMaterialRepository generationSourceMaterialRepository;

    @Autowired
    private QuizRepository quizRepository;

    /** Creates a real User row so project FK references resolve correctly. */
    private Integer persistUser() {
        return authService.createUser("Test", "User", "tuser", "pass").getId();
    }

    // ---------------------------------------------------------------
    // createProject
    // ---------------------------------------------------------------

    /**
     * A newly created project should be persisted and have a generated UUID.
     */
    @Test
    void createProject_persistsProjectWithGeneratedId() {
        Integer userId = persistUser();

        Project project = projectService.createProject(userId, "Intro to Java");

        assertThat(project.getId()).isNotNull();
        assertThat(projectRepository.findById(project.getId())).isPresent();
    }

    /**
     * The returned project should carry the userId and name that were passed in.
     */
    @Test
    void createProject_storesCorrectUserIdAndName() {
        Integer userId = persistUser();

        Project project = projectService.createProject(userId, "Intro to Java");

        assertThat(project.getUserId().getId()).isEqualTo(userId);
        assertThat(project.getName()).isEqualTo("Intro to Java");
    }

    // ---------------------------------------------------------------
    // getProject
    // ---------------------------------------------------------------

    /**
     * Fetching a project by its ID should return the same project.
     */
    @Test
    void getProject_returnsProject_whenExists() {
        Integer userId = persistUser();
        Project created = projectService.createProject(userId, "Intro to Java");

        Project found = projectService.getProject(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent project ID should throw EntityNotFoundException
     * rather than return null or an empty Optional.
     */
    @Test
    void getProject_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.getProject(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // updateProject
    // ---------------------------------------------------------------

    /**
     * The name field should reflect the new value after update.
     */
    @Test
    void updateProject_updatesName() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Old Name");

        Project updated = projectService.updateProject(project.getId(), "New Name");

        assertThat(updated.getName()).isEqualTo("New Name");
    }

    /**
     * Updating a non-existent project ID should throw EntityNotFoundException.
     */
    @Test
    void updateProject_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.updateProject(UUID.randomUUID(), "New Name"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // deleteProject
    // ---------------------------------------------------------------

    /**
     * Deleting a project should remove the project row from the database.
     */
    @Test
    void deleteProject_removesProjectRow() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        UUID projectId = project.getId();

        assertThat(projectRepository.findById(projectId)).isPresent();

        projectService.deleteProject(projectId);

        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.findById(projectId)).isEmpty();
    }

    /**
     * Deleting a non-existent project should throw EntityNotFoundException rather
     * than silently succeed. deleteProject is the single entry point for
     * future ownership checks, so it must always validate the ID exists.
     */
    @Test
    void deleteProject_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.deleteProject(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /**
     * Cascade delete: removing a project should also remove any source materials
     * associated with it.
     */
    @Test
    void deleteProject_cascadesToSourceMaterials() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        SourceMaterial material = projectService.createSourceMaterial(
                project.getId(), "notes.pdf", "application/pdf", new byte[]{1, 2, 3});
        UUID materialId = material.getId();

        entityManager.flush();
        entityManager.clear();

        projectService.deleteProject(project.getId());



        assertThat(sourceMaterialRepository.findById(materialId)).isEmpty();
    }

    /**
     * Cascade delete: removing a project should also remove any generations
     * (and their descendant quizzes) associated with it.
     */
    @Test
    void deleteProject_cascadesToGenerations() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        UUID generationId = generation.getId();

        entityManager.flush();
        entityManager.clear();

        projectService.deleteProject(project.getId());

        assertThat(generationRepository.findById(generationId)).isEmpty();
    }

    /**
     * Deletes a source material by ID. The service is the single entry point
     * for deletion so that ownership checks and other pre-deletion logic have
     * one place to live when added later.
     */
    @Transactional
    public void deleteSourceMaterial(UUID sourceMaterialId) {
        SourceMaterial material = sourceMaterialRepository.findById(sourceMaterialId)
                                                          .orElseThrow(() -> new EntityNotFoundException("Source material not found: " + sourceMaterialId));
        sourceMaterialRepository.delete(material);
    }

    // ---------------------------------------------------------------
    // createSourceMaterial
    // ---------------------------------------------------------------

    /**
     * Creating a source material should persist it and derive fileSizeBytes
     * from the length of the uploaded byte array.
     */
    @Test
    void createSourceMaterial_persistsMaterialWithCorrectMetadata() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        byte[] data = {1, 2, 3, 4};

        SourceMaterial material = projectService.createSourceMaterial(
                project.getId(), "slides.pdf", "application/pdf", data);

        assertThat(material.getId()).isNotNull();
        assertThat(sourceMaterialRepository.findById(material.getId())).isPresent();
        assertThat(material.getFilename()).isEqualTo("slides.pdf");
        assertThat(material.getFileType()).isEqualTo("application/pdf");
        assertThat(material.getFileSizeBytes()).isEqualTo(4);
    }

    /**
     * Passing a non-existent project ID should throw EntityNotFoundException
     * before any persistence is attempted.
     */
    @Test
    void createSourceMaterial_throwsEntityNotFoundException_whenProjectNotFound() {
        assertThatThrownBy(() -> projectService.createSourceMaterial(
                UUID.randomUUID(), "slides.pdf", "application/pdf", new byte[]{1}))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // getSourceMaterial
    // ---------------------------------------------------------------

    /**
     * Fetching a source material by its ID should return the correct record.
     */
    @Test
    void getSourceMaterial_returnsMaterial_whenExists() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        SourceMaterial created = projectService.createSourceMaterial(
                project.getId(), "notes.txt", "text/plain", new byte[]{42});

        SourceMaterial found = projectService.getSourceMaterial(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent source material ID should throw EntityNotFoundException.
     */
    @Test
    void getSourceMaterial_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.getSourceMaterial(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // deleteSourceMaterial
    // ---------------------------------------------------------------

    /**
     * Deleting a source material should remove the row from the database.
     */
    @Test
    void deleteSourceMaterial_removesMaterialRow() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        SourceMaterial material = projectService.createSourceMaterial(
                project.getId(), "notes.pdf", "application/pdf", new byte[]{1, 2, 3});
        UUID materialId = material.getId();

        projectService.deleteSourceMaterial(materialId);

        entityManager.flush();
        entityManager.clear();

        assertThat(sourceMaterialRepository.findById(materialId)).isEmpty();
    }

    // ---------------------------------------------------------------
    // createGeneration
    // ---------------------------------------------------------------

    /**
     * Creating a generation should persist it linked to the specified project.
     */
    @Test
    void createGeneration_persistsGenerationLinkedToProject() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");

        Generation generation = projectService.createGeneration(project.getId(), List.of());

        assertThat(generation.getId()).isNotNull();
        assertThat(generationRepository.findById(generation.getId())).isPresent();
    }

    /**
     * Each source material ID in the input list should produce a join record
     * linking it to the new generation. This verifies the N+1-safe bulk load
     * path in createGeneration.
     */
    @Test
    void createGeneration_linksSelectedSourceMaterials() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        SourceMaterial mat1 = projectService.createSourceMaterial(
                project.getId(), "a.pdf", "application/pdf", new byte[]{1});
        SourceMaterial mat2 = projectService.createSourceMaterial(
                project.getId(), "b.pdf", "application/pdf", new byte[]{2});

        projectService.createGeneration(project.getId(), List.of(mat1.getId(), mat2.getId()));

        assertThat(generationSourceMaterialRepository.count()).isEqualTo(2);
    }

    /**
     * Passing a non-existent project ID should throw EntityNotFoundException.
     */
    @Test
    void createGeneration_throwsEntityNotFoundException_whenProjectNotFound() {
        assertThatThrownBy(() -> projectService.createGeneration(UUID.randomUUID(), List.of()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // getGeneration
    // ---------------------------------------------------------------

    /**
     * Fetching a generation by its ID should return the correct record.
     */
    @Test
    void getGeneration_returnsGeneration_whenExists() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation created = projectService.createGeneration(project.getId(), List.of());

        Generation found = projectService.getGeneration(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent generation ID should throw EntityNotFoundException.
     */
    @Test
    void getGeneration_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.getGeneration(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // deleteGeneration
    // ---------------------------------------------------------------

    /**
     * Deleting a generation should remove the row from the database.
     */
    @Test
    void deleteGeneration_removesGenerationRow() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        UUID generationId = generation.getId();

        projectService.deleteGeneration(generationId);

        entityManager.flush();
        entityManager.clear();

        assertThat(generationRepository.findById(generationId)).isEmpty();
    }

    // ---------------------------------------------------------------
    // createQuiz
    // ---------------------------------------------------------------

    /**
     * Creating a quiz should persist it linked to the specified generation,
     * storing the provided name and raw AI output verbatim.
     */
    @Test
    void createQuiz_persistsQuizWithCorrectContent() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation generation = projectService.createGeneration(project.getId(), List.of());

        Quiz quiz = projectService.createQuiz(generation.getId(), "Week 1 Quiz", "{\"questions\":[]}");

        assertThat(quiz.getQuiz_id()).isNotNull();
        assertThat(quizRepository.findById(quiz.getQuiz_id())).isPresent();
        assertThat(quiz.getName()).isEqualTo("Week 1 Quiz");
        assertThat(quiz.getRawContent()).isEqualTo("{\"questions\":[]}");
    }

    /**
     * Passing a non-existent generation ID should throw EntityNotFoundException.
     */
    @Test
    void createQuiz_throwsEntityNotFoundException_whenGenerationNotFound() {
        assertThatThrownBy(() -> projectService.createQuiz(UUID.randomUUID(), "Quiz", "content"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // getQuiz
    // ---------------------------------------------------------------

    /**
     * Fetching a quiz by its ID should return the correct record.
     */
    @Test
    void getQuiz_returnsQuiz_whenExists() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        Quiz created = projectService.createQuiz(generation.getId(), "Week 1 Quiz", "raw content");

        Quiz found = projectService.getQuiz(created.getQuiz_id());

        assertThat(found.getQuiz_id()).isEqualTo(created.getQuiz_id());
    }

    /**
     * Fetching a non-existent quiz ID should throw EntityNotFoundException.
     */
    @Test
    void getQuiz_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.getQuiz(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // updateQuiz
    // ---------------------------------------------------------------

    /**
     * Both name and rawContent should reflect the new values after update.
     */
    @Test
    void updateQuiz_updatesNameAndRawContent() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        Quiz quiz = projectService.createQuiz(generation.getId(), "Old Name", "old content");

        Quiz updated = projectService.updateQuiz(quiz.getQuiz_id(), "New Name", "new content");

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getRawContent()).isEqualTo("new content");
    }

    /**
     * Updating a non-existent quiz ID should throw EntityNotFoundException.
     */
    @Test
    void updateQuiz_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> projectService.updateQuiz(UUID.randomUUID(), "Name", "content"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // deleteQuiz
    // ---------------------------------------------------------------

    /**
     * Deleting a quiz should remove the row from the database.
     */
    @Test
    void deleteQuiz_removesQuizRow() {
        Integer userId = persistUser();
        Project project = projectService.createProject(userId, "Intro to Java");
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        Quiz quiz = projectService.createQuiz(generation.getId(), "Week 1 Quiz", "content");
        UUID quizId = quiz.getQuiz_id();

        projectService.deleteQuiz(quizId);

        entityManager.flush();
        entityManager.clear();

        assertThat(quizRepository.findById(quizId)).isEmpty();
    }
}
