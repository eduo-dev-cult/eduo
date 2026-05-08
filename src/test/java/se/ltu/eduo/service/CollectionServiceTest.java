package se.ltu.eduo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.TestContainersInitializer;
import se.ltu.eduo.model.collection.*;
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
class CollectionServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CollectionRepository collectionRepository;

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
    void createProject_persistsCollectionWithGeneratedId() {
        Integer userId = persistUser();

        Collection collection = collectionService.createCollection(userId, "Intro to Java");

        assertThat(collection.getId()).isNotNull();
        assertThat(collectionRepository.findById(collection.getId())).isPresent();
    }

    /**
     * The returned project should carry the owner's userId and name that were passed in.
     */
    @Test
    void createCollection_storesCorrectUserIdAndName() {
        Integer userId = persistUser();

        Collection collection = collectionService.createCollection(userId, "Intro to Java");

        assertThat(collection.getOwner().getId()).isEqualTo(userId);
        assertThat(collection.getName()).isEqualTo("Intro to Java");
    }

    // ---------------------------------------------------------------
    // getProject
    // ---------------------------------------------------------------

    /**
     * Fetching a project by its ID should return the same project.
     */
    @Test
    void getProject_returnsCollection_whenExists() {
        Integer userId = persistUser();
        Collection created = collectionService.createCollection(userId, "Intro to Java");

        Collection found = collectionService.getCollection(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent project ID should throw EntityNotFoundException
     * rather than return null or an empty Optional.
     */
    @Test
    void getCollection_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.getCollection(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // updateProject
    // ---------------------------------------------------------------

    /**
     * The name field should reflect the new value after update.
     */
    @Test
    void updateCollection_updatesName() {
        Integer userId = persistUser();
        Collection collection = collectionService.createCollection(userId, "Old Name");

        Collection updated = collectionService.updateCollection(collection.getId(), "New Name");

        assertThat(updated.getName()).isEqualTo("New Name");
    }

    /**
     * Updating a non-existent project ID should throw EntityNotFoundException.
     */
    @Test
    void updateCollection_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.updateCollection(UUID.randomUUID(), "New Name"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // deleteProject
    // ---------------------------------------------------------------

    /**
     * Deleting a project should remove the project row from the database.
     */
    @Test
    void deleteProject_removesCollectionRow() {
        Integer userId = persistUser();
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        UUID projectId = collection.getId();

        assertThat(collectionRepository.findById(projectId)).isPresent();

        collectionService.deleteCollection(projectId);

        entityManager.flush();
        entityManager.clear();

        assertThat(collectionRepository.findById(projectId)).isEmpty();
    }

    /**
     * Deleting a non-existent project should throw EntityNotFoundException rather
     * than silently succeed. deleteProject is the single entry point for
     * future ownership checks, so it must always validate the ID exists.
     */
    @Test
    void deleteCollection_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.deleteCollection(UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    /**
     * Cascade delete: removing a project should also remove any source materials
     * associated with it.
     */
    @Test
    void deleteCollection_cascadesToSourceMaterials() {
        Integer userId = persistUser();
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        SourceMaterial material = collectionService.createSourceMaterial(
                collection.getId(), "notes.pdf", "application/pdf", new byte[]{1, 2, 3});
        UUID materialId = material.getId();

        entityManager.flush();
        entityManager.clear();

        collectionService.deleteCollection(collection.getId());



        assertThat(sourceMaterialRepository.findById(materialId)).isEmpty();
    }

    /**
     * Cascade delete: removing a project should also remove any generations
     * (and their descendant quizzes) associated with it.
     */
    @Test
    void deleteCollection_cascadesToGenerations() {
        Integer userId = persistUser();
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation generation = collectionService.createGeneration(collection.getId(), List.of());
        UUID generationId = generation.getId();

        entityManager.flush();
        entityManager.clear();

        collectionService.deleteCollection(collection.getId());

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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        byte[] data = {1, 2, 3, 4};

        SourceMaterial material = collectionService.createSourceMaterial(
                collection.getId(), "slides.pdf", "application/pdf", data);

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
        assertThatThrownBy(() -> collectionService.createSourceMaterial(
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        SourceMaterial created = collectionService.createSourceMaterial(
                collection.getId(), "notes.txt", "text/plain", new byte[]{42});

        SourceMaterial found = collectionService.getSourceMaterial(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent source material ID should throw EntityNotFoundException.
     */
    @Test
    void getSourceMaterial_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.getSourceMaterial(UUID.randomUUID()))
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        SourceMaterial material = collectionService.createSourceMaterial(
                collection.getId(), "notes.pdf", "application/pdf", new byte[]{1, 2, 3});
        UUID materialId = material.getId();

        collectionService.deleteSourceMaterial(materialId);

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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");

        Generation generation = collectionService.createGeneration(collection.getId(), List.of());

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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        SourceMaterial mat1 = collectionService.createSourceMaterial(
                collection.getId(), "a.pdf", "application/pdf", new byte[]{1});
        SourceMaterial mat2 = collectionService.createSourceMaterial(
                collection.getId(), "b.pdf", "application/pdf", new byte[]{2});

        collectionService.createGeneration(collection.getId(), List.of(mat1.getId(), mat2.getId()));

        assertThat(generationSourceMaterialRepository.count()).isEqualTo(2);
    }

    /**
     * Passing a non-existent project ID should throw EntityNotFoundException.
     */
    @Test
    void createGeneration_throwsEntityNotFoundException_whenProjectNotFound() {
        assertThatThrownBy(() -> collectionService.createGeneration(UUID.randomUUID(), List.of()))
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation created = collectionService.createGeneration(collection.getId(), List.of());

        Generation found = collectionService.getGeneration(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent generation ID should throw EntityNotFoundException.
     */
    @Test
    void getGeneration_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.getGeneration(UUID.randomUUID()))
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation generation = collectionService.createGeneration(collection.getId(), List.of());
        UUID generationId = generation.getId();

        collectionService.deleteGeneration(generationId);

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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation generation = collectionService.createGeneration(collection.getId(), List.of());

        Quiz quiz = collectionService.createQuiz(generation.getId(), "Week 1 Quiz", "{\"questions\":[]}");

        assertThat(quiz.getId()).isNotNull();
        assertThat(quizRepository.findById(quiz.getId())).isPresent();
        assertThat(quiz.getName()).isEqualTo("Week 1 Quiz");
        assertThat(quiz.getRawContent()).isEqualTo("{\"questions\":[]}");
    }

    /**
     * Passing a non-existent generation ID should throw EntityNotFoundException.
     */
    @Test
    void createQuiz_throwsEntityNotFoundException_whenGenerationNotFound() {
        assertThatThrownBy(() -> collectionService.createQuiz(UUID.randomUUID(), "Quiz", "content"))
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation generation = collectionService.createGeneration(collection.getId(), List.of());
        Quiz created = collectionService.createQuiz(generation.getId(), "Week 1 Quiz", "raw content");

        Quiz found = collectionService.getQuiz(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
    }

    /**
     * Fetching a non-existent quiz ID should throw EntityNotFoundException.
     */
    @Test
    void getQuiz_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.getQuiz(UUID.randomUUID()))
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation generation = collectionService.createGeneration(collection.getId(), List.of());
        Quiz quiz = collectionService.createQuiz(generation.getId(), "Old Name", "old content");

        Quiz updated = collectionService.updateQuiz(quiz.getId(), "New Name", "new content");

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getRawContent()).isEqualTo("new content");
    }

    /**
     * Updating a non-existent quiz ID should throw EntityNotFoundException.
     */
    @Test
    void updateQuiz_throwsEntityNotFoundException_whenNotFound() {
        assertThatThrownBy(() -> collectionService.updateQuiz(UUID.randomUUID(), "Name", "content"))
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
        Collection collection = collectionService.createCollection(userId, "Intro to Java");
        Generation generation = collectionService.createGeneration(collection.getId(), List.of());
        Quiz quiz = collectionService.createQuiz(generation.getId(), "Week 1 Quiz", "content");
        UUID quizId = quiz.getId();

        collectionService.deleteQuiz(quizId);

        entityManager.flush();
        entityManager.clear();

        assertThat(quizRepository.findById(quizId)).isEmpty();
    }
}
