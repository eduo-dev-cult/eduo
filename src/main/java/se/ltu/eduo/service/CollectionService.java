package se.ltu.eduo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.collection.*;
import se.ltu.eduo.repository.*;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates creation, retrieval, and deletion of collections and their
 * associated domain objects. All methods that write to the database are
 * transactional. Read methods are marked read-only as a performance hint
 * to the persistence provider.
 *
 * <p>Callers receive the saved or created entity.
 */
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final SourceMaterialRepository sourceMaterialRepository;
    private final GenerationRepository generationRepository;
    private final GenerationSourceMaterialRepository generationSourceMaterialRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    //region collections
    // Standard CRUD behaviours.
    // -------------------------------------------------------------------------

    /**
     * Creates and persists a new collection owned by the given user.
     * Time of creation is that of the invocation.
     * @param userId the owner of the collection
     * @param name the name of the collection
     * @return the created collection
     */
    @Transactional
    public Collection createCollection(Integer userId, String name) throws EntityNotFoundException
    {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new EntityNotFoundException("User not found by this ID: " + userId));
        Collection collection = new Collection(user, name);
        return collectionRepository.save(collection);
    }

    /**
     * Returns a collection by ID, throwing if it does not exist.
     */
    @Transactional(readOnly = true)
    public Collection getCollection(UUID collectionId) throws EntityNotFoundException{
        return collectionRepository.findById(collectionId)
                                   .orElseThrow(() -> new EntityNotFoundException("collection not found with this ID: " + collectionId));
    }

    /**
     * Updates the name of an existing collection and returns the saved state.
     * {@code updatedAt} is managed by {@link org.hibernate.annotations.UpdateTimestamp}
     * and refreshes automatically on save.
     */
    @Transactional
    public Collection updateCollection(UUID collectionId, String name) {
        Collection collection = collectionRepository.findById(collectionId)
                                                    .orElseThrow(() -> new EntityNotFoundException("collection not found with this ID: " + collectionId));
        collection.setName(name);
        return collectionRepository.save(collection);
    }

    /**
     * Deletes a collection and all of its associated source materials, generations,
     * and quizzes. Cascade rules on the entity handle child deletion — this
     * method is the single authorised entry point for collection deletion so that
     * any future pre-deletion checks (e.g. ownership verification) have one
     * place to live.
     */
    @Transactional
    public void deleteCollection(UUID collectionId) {
        Collection collection = collectionRepository.findById(collectionId)
                                                    .orElseThrow(() -> new EntityNotFoundException("collection not found with this ID: " + collectionId));
        collectionRepository.delete(collection);
    }

    //endregion collections

    // -------------------------------------------------------------------------
    //region Source material
    // Almost standard - no update as delete and reupload fits that pattern.
    // -------------------------------------------------------------------------

    /**
     * Uploads a file and associates it with the given collection.
     */
    @Transactional
    public SourceMaterial createSourceMaterial(UUID collectionId,
                                               String filename,
                                               String fileType,
                                               byte[] fileData) {
        Collection collection = collectionRepository.findById(collectionId)
                                                    .orElseThrow(() -> new EntityNotFoundException("collection not found with this ID: " + collectionId));
        SourceMaterial material = new SourceMaterial(collection, filename, fileType, fileData);
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
     * Creates a generation attempt for the given collection, recording which
     * source materials were selected for this run. Loads all materials in a
     * single query via {@code findAllById} to avoid N+1 lookups.
     *
     * <p>The caller is responsible for subsequently calling
     * {@link #createQuiz(UUID, String, String)} once the AI response is available.
     */
    @Transactional
    public Generation createGeneration(UUID collectionId, List<UUID> sourceMaterialIds) {
        Collection collection = collectionRepository.findById(collectionId)
                                                    .orElseThrow(() -> new EntityNotFoundException("collection not found: " + collectionId));

        Generation generation = generationRepository.save(new Generation(collection));

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
