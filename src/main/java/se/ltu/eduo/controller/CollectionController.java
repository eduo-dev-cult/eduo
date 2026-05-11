package se.ltu.eduo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.ltu.eduo.dto.*;
import se.ltu.eduo.dto.request.CreateCollectionRequest;
import se.ltu.eduo.dto.request.CreateGenerationRequest;
import se.ltu.eduo.dto.request.UpdateProjectRequest;
import se.ltu.eduo.dto.request.UpdateQuizRequest;
import se.ltu.eduo.mapper.CollectionMapper;
import se.ltu.eduo.mapper.GenerationMapper;
import se.ltu.eduo.mapper.QuizMapper;
import se.ltu.eduo.model.collection.*;
import se.ltu.eduo.service.CollectionService;
import se.ltu.eduo.service.StudyQuestionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private static final Logger logger =  LoggerFactory.getLogger(CollectionController.class);

    private final CollectionService collectionService;
    private final CollectionMapper collectionMapper;
    private final GenerationMapper generationMapper;
    private final QuizMapper quizMapper;
    private final StudyQuestionService studyQuestionService;

    // -------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<CollectionDto> createCollection(@RequestBody CreateCollectionRequest request) {
        //fixme ide reports xss risk in method
        if(request.name() == null || request.name().isBlank()) {return  ResponseEntity.badRequest().build();}
        Collection collection = collectionService.createCollection(request.userId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionMapper.toDto(collection));
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<CollectionDto> getProject(@PathVariable UUID collectionId) {
        return ResponseEntity.ok(collectionMapper.toDto(collectionService.getCollection(collectionId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CollectionDto>> getUserCollections(@PathVariable Integer userId) {
        List<CollectionDto> collections = collectionService.getUserCollections(userId)
                                                           .stream()
                                                           .map(collectionMapper::toDto)
                                                           .toList();
        return ResponseEntity.ok().body(collections);
    }

    @PatchMapping("/{collectionId}")
    public ResponseEntity<CollectionDto> updateProject(@PathVariable UUID collectionId,
                                                       @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(collectionMapper.toDto(collectionService.updateCollection(collectionId, request.name())));
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID collectionId) {
        collectionService.deleteCollection(collectionId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Source materials
    // -------------------------------------------------------------------------

    @PostMapping("/{collectionId}/materials")
    public ResponseEntity<CollectionDto.SourceMaterialDto> uploadMaterial(@PathVariable UUID collectionId,
                                                                          @RequestParam("file") MultipartFile file) throws IOException {
        //fixme ide reports xss risk in method
        SourceMaterial material = collectionService.createSourceMaterial(
                collectionId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSourceMaterialDto(material));
    }

    @GetMapping("/{collectionId}/materials/{materialId}")
    public ResponseEntity<byte[]> downloadMaterial(@PathVariable UUID materialId) {
        SourceMaterial material = collectionService.getSourceMaterial(materialId);
        String safeFilename = material.getFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
                .body(material.getFileData());
    }

    @DeleteMapping("/{collectionId}/materials/{materialId}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable UUID materialId) {
        collectionService.deleteSourceMaterial(materialId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Generations — creating one triggers the LLM and persists the quiz result
    // -------------------------------------------------------------------------

    @PostMapping("/{collectionId}/generations")
    public ResponseEntity<GenerationDto> createGeneration(@PathVariable UUID collectionId,
                                                          @Valid @RequestBody CreateGenerationRequest request) {
        logger.atDebug().log("Received request to create generation for collection");
        //step 1 validate data - handled by @Valid annotation in method header

        //step 2 call StudyQuestionService with request
        //TODO proper request type
        //GenerationDto generationdto = studyQuestionService.generateStudyQuestions(request);

        //step 3 return generationdto containing generated quiz
        return ResponseEntity.status(HttpStatus.CREATED).build(); //fixme should have content
    }

    @GetMapping("/{collectionId}/generations/{generationId}")
    public ResponseEntity<GenerationDto> getGeneration(@PathVariable UUID generationId) {
        return ResponseEntity.ok(generationMapper.toDto(collectionService.getGeneration(generationId)));
    }

    @DeleteMapping("/{collectionId}/generations/{generationId}")
    public ResponseEntity<Void> deleteGeneration(@PathVariable UUID generationId) {
        collectionService.deleteGeneration(generationId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Quiz — one per generation, editable for future manual corrections
    // -------------------------------------------------------------------------

    @GetMapping("/{collectionId}/generations/{generationId}/quiz")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable UUID generationId) {
        Quiz quiz = collectionService.getGeneration(generationId).getQuiz();
        if (quiz == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(quizMapper.toDto(quiz));
    }

    @PatchMapping("/{collectionId}/generations/{generationId}/quiz")
    public ResponseEntity<QuizDto> updateQuiz(@PathVariable UUID generationId,
                                              @RequestBody UpdateQuizRequest request) {
        Quiz quiz = collectionService.getGeneration(generationId).getQuiz();
        if (quiz == null) return ResponseEntity.notFound().build();
        Quiz updated = collectionService.updateQuiz(quiz.getId(), request.name(), request.rawContent());
        return ResponseEntity.ok(quizMapper.toDto(updated));
    }

    @DeleteMapping("/{collectionId}/generations/{generationId}/quiz")
    public ResponseEntity<Void> deleteQuiz(@PathVariable UUID generationId) {
        Quiz quiz = collectionService.getGeneration(generationId).getQuiz();
        if (quiz == null) return ResponseEntity.notFound().build();
        collectionService.deleteQuiz(quiz.getId());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------

    private CollectionDto.SourceMaterialDto toSourceMaterialDto(SourceMaterial m) {
        return new CollectionDto.SourceMaterialDto(m.getId(), m.getFilename(), m.getFileType(),
                                                   m.getFileSizeBytes(), m.getUploadedAt());
    }
}