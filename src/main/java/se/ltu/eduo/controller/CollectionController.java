package se.ltu.eduo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.ltu.eduo.dto.*;
import se.ltu.eduo.mapper.CollectionMapper;
import se.ltu.eduo.mapper.GenerationMapper;
import se.ltu.eduo.mapper.QuizMapper;
import se.ltu.eduo.model.collection.*;
import se.ltu.eduo.service.LlmService;
import se.ltu.eduo.service.CollectionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final LlmService llmService;
    private final CollectionMapper collectionMapper;
    private final GenerationMapper generationMapper;
    private final QuizMapper quizMapper;

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
                                                          @RequestBody CreateGenerationRequest request) {
        //TODO AIn var lite för snabb med att bygga prompts utan att använda det planerade systemet. Måste fixas.
        Generation generation = collectionService.createGeneration(collectionId, request.sourceMaterialIds());

        String prompt = request.sourceMaterialIds().stream()
                .map(collectionService::getSourceMaterial)
                .map(m -> new String(m.getFileData(), StandardCharsets.UTF_8))
                .reduce("", (a, b) -> a + "\n\n" + b);

        String rawContent = llmService.generateStudyQuestions(prompt);
        Quiz quiz = collectionService.createQuiz(generation.getId(), "Quiz", rawContent);
        // Wire the quiz onto the in-memory entity so the mapper sees it without
        // a re-fetch (re-fetching returns a stale L1-cached entity with quiz=null).
        generation.setQuiz(quiz);

        return ResponseEntity.status(HttpStatus.CREATED).body(generationMapper.toDto(generation));
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