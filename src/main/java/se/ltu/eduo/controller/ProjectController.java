package se.ltu.eduo.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import se.ltu.eduo.dto.*;
import se.ltu.eduo.mapper.GenerationMapper;
import se.ltu.eduo.mapper.ProjectMapper;
import se.ltu.eduo.mapper.QuizMapper;
import se.ltu.eduo.model.project.Generation;
import se.ltu.eduo.model.project.Project;
import se.ltu.eduo.model.project.Quiz;
import se.ltu.eduo.model.project.SourceMaterial;
import se.ltu.eduo.service.LlmService;
import se.ltu.eduo.service.ProjectService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final LlmService llmService;
    private final ProjectMapper projectMapper;
    private final GenerationMapper generationMapper;
    private final QuizMapper quizMapper;

    // -------------------------------------------------------------------------
    // Projects
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody CreateProjectRequest request) {
        //fixme ide reports xss risk in method
        if(request.name() == null || request.name().isBlank()) {return  ResponseEntity.badRequest().build();}
        try {
            Project project = projectService.createProject(request.userId(), request.name());
            return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.toDto(project));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable UUID projectId) {
        try
        {
            return ResponseEntity.ok(projectMapper.toDto(projectService.getProject(projectId)));
        } catch (EntityNotFoundException e){
            return ResponseEntity.notFound().build();
        }

    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable UUID projectId,
                                                    @RequestBody UpdateProjectRequest request) {
        try {
            return ResponseEntity.ok(projectMapper.toDto(projectService.updateProject(projectId, request.name())));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Source materials
    // -------------------------------------------------------------------------

    @PostMapping("/{projectId}/materials")
    public ResponseEntity<ProjectDto.SourceMaterialDto> uploadMaterial(@PathVariable UUID projectId,
                                                                       @RequestParam("file") MultipartFile file) throws IOException {
        //fixme ide reports xss risk in method
        SourceMaterial material = projectService.createSourceMaterial(
                projectId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(toSourceMaterialDto(material));
    }

    @GetMapping("/{projectId}/materials/{materialId}")
    public ResponseEntity<byte[]> downloadMaterial(@PathVariable UUID materialId) {
        SourceMaterial material = projectService.getSourceMaterial(materialId);
        String safeFilename = material.getFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(material.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
                .body(material.getFileData());
    }

    @DeleteMapping("/{projectId}/materials/{materialId}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable UUID materialId) {
        projectService.deleteSourceMaterial(materialId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Generations — creating one triggers the LLM and persists the quiz result
    // -------------------------------------------------------------------------

    @PostMapping("/{projectId}/generations")
    public ResponseEntity<GenerationDto> createGeneration(@PathVariable UUID projectId,
                                                          @RequestBody CreateGenerationRequest request) {
        //TODO AIn var lite för snabb med att bygga prompts utan att använda det planerade systemet. Måste fixas.
        Generation generation = projectService.createGeneration(projectId, request.sourceMaterialIds());

        String prompt = request.sourceMaterialIds().stream()
                .map(projectService::getSourceMaterial)
                .map(m -> new String(m.getFileData(), StandardCharsets.UTF_8))
                .reduce("", (a, b) -> a + "\n\n" + b);

        String rawContent = llmService.generateStudyQuestions(prompt);
        Quiz quiz = projectService.createQuiz(generation.getId(), "Quiz", rawContent);
        // Wire the quiz onto the in-memory entity so the mapper sees it without
        // a re-fetch (re-fetching returns a stale L1-cached entity with quiz=null).
        generation.setQuiz(quiz);

        return ResponseEntity.status(HttpStatus.CREATED).body(generationMapper.toDto(generation));
    }

    @GetMapping("/{projectId}/generations/{generationId}")
    public ResponseEntity<GenerationDto> getGeneration(@PathVariable UUID generationId) {
        try
        {
            return ResponseEntity.ok(generationMapper.toDto(projectService.getGeneration(generationId)));
        } catch (EntityNotFoundException e)
        {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{projectId}/generations/{generationId}")
    public ResponseEntity<Void> deleteGeneration(@PathVariable UUID generationId) {
        projectService.deleteGeneration(generationId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Quiz — one per generation, editable for future manual corrections
    // -------------------------------------------------------------------------

    @GetMapping("/{projectId}/generations/{generationId}/quiz")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable UUID generationId) {
        Quiz quiz = projectService.getGeneration(generationId).getQuiz();
        if (quiz == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(quizMapper.toDto(quiz));
    }

    @PatchMapping("/{projectId}/generations/{generationId}/quiz")
    public ResponseEntity<QuizDto> updateQuiz(@PathVariable UUID generationId,
                                              @RequestBody UpdateQuizRequest request) {
        Quiz quiz = projectService.getGeneration(generationId).getQuiz();
        if (quiz == null) return ResponseEntity.notFound().build();
        Quiz updated = projectService.updateQuiz(quiz.getQuiz_id(), request.name(), request.rawContent());
        return ResponseEntity.ok(quizMapper.toDto(updated));
    }

    @DeleteMapping("/{projectId}/generations/{generationId}/quiz")
    public ResponseEntity<Void> deleteQuiz(@PathVariable UUID generationId) {
        Quiz quiz = projectService.getGeneration(generationId).getQuiz();
        if (quiz == null) return ResponseEntity.notFound().build();
        projectService.deleteQuiz(quiz.getQuiz_id());
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------

    private ProjectDto.SourceMaterialDto toSourceMaterialDto(SourceMaterial m) {
        return new ProjectDto.SourceMaterialDto(m.getId(), m.getFilename(), m.getFileType(),
                m.getFileSizeBytes(), m.getUploadedAt());
    }
}