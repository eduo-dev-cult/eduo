package se.ltu.eduo.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.TestContainersInitializer;
import se.ltu.eduo.model.project.Generation;
import se.ltu.eduo.model.project.Project;
import se.ltu.eduo.model.project.SourceMaterial;
import se.ltu.eduo.service.AuthService;
import se.ltu.eduo.service.ProjectService;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProjectService projectService;

    private Integer persistUser() {
        return authService.createUser("Test", "User", "tuser", "pass").getId();
    }

    private Project persistProject(Integer userId) {
        return projectService.createProject(userId, "Test Project");
    }

    private SourceMaterial persistMaterial(UUID projectId) {
        return projectService.createSourceMaterial(
                projectId, "notes.txt", "text/plain", "hello world".getBytes());
    }

    // ---------------------------------------------------------------
    // POST /projects
    // ---------------------------------------------------------------

    @Test
    void createProject_returns201WithProjectData() throws Exception {
        Integer userId = persistUser();

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"name":"Algorithms 101"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Algorithms 101"));
    }

    @Test
    void createProject_returns404_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"name":"Orphan"}
                                """.formatted(Integer.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // GET /projects/{projectId}
    // ---------------------------------------------------------------

    @Test
    void getProject_returns200WithProjectData_whenExists() throws Exception {
        Project project = persistProject(persistUser());

        mockMvc.perform(get("/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    void getProject_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/projects/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // PATCH /projects/{projectId}
    // ---------------------------------------------------------------

    @Test
    void updateProject_returns200WithUpdatedName() throws Exception {
        Project project = persistProject(persistUser());

        mockMvc.perform(patch("/projects/{id}", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed Project"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Project"));
    }

    @Test
    void updateProject_returns404_whenNotFound() throws Exception {
        mockMvc.perform(patch("/projects/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ghost"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /projects/{projectId}
    // ---------------------------------------------------------------

    @Test
    void deleteProject_returns204() throws Exception {
        Project project = persistProject(persistUser());

        mockMvc.perform(delete("/projects/{id}", project.getId()))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // POST /projects/{projectId}/materials  (multipart upload)
    // ---------------------------------------------------------------

    @Test
    void uploadMaterial_returns201WithMaterialMetadata() throws Exception {
        Project project = persistProject(persistUser());
        MockMultipartFile file = new MockMultipartFile(
                "file", "slides.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/projects/{id}/materials", project.getId()).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("slides.pdf"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSizeBytes").value(3));
    }

    // ---------------------------------------------------------------
    // GET /projects/{projectId}/materials/{materialId}
    // ---------------------------------------------------------------

    @Test
    void downloadMaterial_returns200WithFileBytes_whenExists() throws Exception {
        Project project = persistProject(persistUser());
        SourceMaterial material = persistMaterial(project.getId());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/projects/{pid}/materials/{mid}", project.getId(), material.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("notes.txt")))
                .andExpect(content().bytes("hello world".getBytes()));
    }

    @Test
    @Disabled("Downloading uploaded material does not need support at this stage.")
    void downloadMaterial_returns404_whenNotFound() throws Exception {
        Project project = persistProject(persistUser());

        mockMvc.perform(get("/projects/{pid}/materials/{mid}", project.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /projects/{projectId}/materials/{materialId}
    // ---------------------------------------------------------------

    @Test
    void deleteMaterial_returns204() throws Exception {
        Project project = persistProject(persistUser());
        SourceMaterial material = persistMaterial(project.getId());

        mockMvc.perform(delete("/projects/{pid}/materials/{mid}", project.getId(), material.getId()))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // POST /projects/{projectId}/generations
    // ---------------------------------------------------------------

    @Test
    void createGeneration_returns201WithEmbeddedQuiz() throws Exception {
        Project project = persistProject(persistUser());
        SourceMaterial material = persistMaterial(project.getId());

        mockMvc.perform(post("/projects/{id}/generations", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceMaterialIds":["%s"]}
                                """.formatted(material.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.quiz").exists())
                .andExpect(jsonPath("$.quiz.rawContent").isString());
    }

    @Test
    void createGeneration_returns201_withEmptySourceMaterials() throws Exception {
        Project project = persistProject(persistUser());

        mockMvc.perform(post("/projects/{id}/generations", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceMaterialIds":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quiz").exists());
    }

    // ---------------------------------------------------------------
    // GET /projects/{projectId}/generations/{generationId}
    // ---------------------------------------------------------------

    @Test
    void getGeneration_returns200_whenExists() throws Exception {
        Project project = persistProject(persistUser());
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/projects/{pid}/generations/{gid}",
                        project.getId(), generation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(generation.getId().toString()));
    }

    @Test
    void getGeneration_returns404_whenNotFound() throws Exception {
        Project project = persistProject(persistUser());

        mockMvc.perform(get("/projects/{pid}/generations/{gid}",
                        project.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /projects/{projectId}/generations/{generationId}
    // ---------------------------------------------------------------

    @Test
    void deleteGeneration_returns204() throws Exception {
        Project project = persistProject(persistUser());
        Generation generation = projectService.createGeneration(project.getId(), List.of());

        mockMvc.perform(delete("/projects/{pid}/generations/{gid}",
                        project.getId(), generation.getId()))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // GET /projects/{projectId}/generations/{generationId}/quiz
    // ---------------------------------------------------------------

    @Test
    void getQuiz_returns200WithQuizContent_whenExists() throws Exception {
        Project project = persistProject(persistUser());
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        projectService.createQuiz(generation.getId(), "Week 1", "raw content");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/projects/{pid}/generations/{gid}/quiz",
                        project.getId(), generation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Week 1"))
                .andExpect(jsonPath("$.rawContent").value("raw content"));
    }

    @Test
    void getQuiz_returns404_whenGenerationHasNoQuiz() throws Exception {
        Project project = persistProject(persistUser());
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/projects/{pid}/generations/{gid}/quiz",
                        project.getId(), generation.getId()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // PATCH /projects/{projectId}/generations/{generationId}/quiz
    // ---------------------------------------------------------------

    @Test
    void updateQuiz_returns200WithUpdatedContent() throws Exception {
        Project project = persistProject(persistUser());
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        projectService.createQuiz(generation.getId(), "Old Name", "old content");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(patch("/projects/{pid}/generations/{gid}/quiz",
                        project.getId(), generation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Name","rawContent":"new content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.rawContent").value("new content"));
    }

    // ---------------------------------------------------------------
    // DELETE /projects/{projectId}/generations/{generationId}/quiz
    // ---------------------------------------------------------------

    @Test
    void deleteQuiz_returns204() throws Exception {
        Project project = persistProject(persistUser());
        Generation generation = projectService.createGeneration(project.getId(), List.of());
        projectService.createQuiz(generation.getId(), "Quiz", "content");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/projects/{pid}/generations/{gid}/quiz",
                        project.getId(), generation.getId()))
                .andExpect(status().isNoContent());
    }
}
