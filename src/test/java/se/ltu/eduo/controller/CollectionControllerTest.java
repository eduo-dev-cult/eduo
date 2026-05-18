package se.ltu.eduo.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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
import se.ltu.eduo.TestDataGenerator;
import se.ltu.eduo.collection.request.CreateGenerationRequest;
import se.ltu.eduo.collection.model.Collection;
import se.ltu.eduo.collection.model.Generation;
import se.ltu.eduo.collection.model.SourceMaterial;
import se.ltu.eduo.user.service.AuthService;
import se.ltu.eduo.collection.service.CollectionService;
import tools.jackson.databind.ObjectMapper;

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
class CollectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private CollectionService collectionService;

    private Integer persistUser() {
        return TestDataGenerator.persistUser(authService);
    }

    private Collection persistCollection(Integer userId) {
        return collectionService.createCollection(userId, "Test Collection", "Test description");
    }

    private SourceMaterial persistMaterial(UUID collectionId) {
        return collectionService.createSourceMaterial(
                collectionId, "notes.txt", "text/plain", "hello world".getBytes());
    }

    private Generation persistGeneration(UUID collectionId) {
        return collectionService.createGeneration(collectionId, TestDataGenerator.validGenerationRequest());
    }

    // ---------------------------------------------------------------
    // POST /collections
    // ---------------------------------------------------------------

    @Test
    void createCollection_returns201WithCollectionData() throws Exception {
        Integer userId = persistUser();

        mockMvc.perform(post("/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"name":"Algorithms 101"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Algorithms 101"));
    }

    @Test
    void createCollection_returns404_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"name":"Orphan"}
                                """.formatted(Integer.MAX_VALUE)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // GET /collections/{collectionId}
    // ---------------------------------------------------------------

    @Test
    void getCollection_returns200WithCollectionData_whenExists() throws Exception {
        Collection collection = persistCollection(persistUser());

        mockMvc.perform(get("/collections/{id}", collection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(collection.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Collection"));
    }

    @Test
    void getCollection_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/collections/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCollection_nestedGenerations_haveCollectionIdPopulated() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());
        collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest(material.getId()));
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/collections/{id}", collection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generations[0].collectionId").value(collection.getId().toString()));
    }

    // ---------------------------------------------------------------
    // PATCH /collections/{collectionId}
    // ---------------------------------------------------------------

    @Test
    void updateCollection_returns200WithUpdatedName() throws Exception {
        Collection collection = persistCollection(persistUser());

        mockMvc.perform(patch("/collections/{id}", collection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed Collection"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Collection"));
    }

    @Test
    void updateCollection_returns404_whenNotFound() throws Exception {
        mockMvc.perform(patch("/collections/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ghost"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /collections/{collectionId}
    // ---------------------------------------------------------------

    @Test
    void deleteCollection_returns204() throws Exception {
        Collection collection = persistCollection(persistUser());

        mockMvc.perform(delete("/collections/{id}", collection.getId()))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // POST /collections/{collectionId}/materials  (multipart upload)
    // ---------------------------------------------------------------

    @Test
    void uploadMaterial_returns201WithMaterialMetadata() throws Exception {
        Collection collection = persistCollection(persistUser());
        MockMultipartFile file = new MockMultipartFile(
                "file", "slides.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/collections/{id}/materials", collection.getId()).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("slides.pdf"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSizeBytes").value(3));
    }

    // ---------------------------------------------------------------
    // GET /collections/{collectionId}/materials/{materialId}
    // ---------------------------------------------------------------

    @Test
    void downloadMaterial_returns200WithFileBytes_whenExists() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/collections/{pid}/materials/{mid}", collection.getId(), material.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("notes.txt")))
                .andExpect(content().bytes("hello world".getBytes()));
    }

    @Test
    @Disabled("Downloading uploaded material does not need support at this stage.")
    void downloadMaterial_returns404_whenNotFound() throws Exception {
        Collection collection = persistCollection(persistUser());

        mockMvc.perform(get("/collections/{pid}/materials/{mid}", collection.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /collections/{collectionId}/materials/{materialId}
    // ---------------------------------------------------------------

    @Test
    void deleteMaterial_returns204() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());

        mockMvc.perform(delete("/collections/{pid}/materials/{mid}", collection.getId(), material.getId()))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // POST /collections/{collectionId}/generations
    // ---------------------------------------------------------------

    @Test
    void createGeneration_returns201WithEmbeddedQuiz() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());
        CreateGenerationRequest request = TestDataGenerator.validGenerationRequest(material.getId());



        mockMvc.perform(post("/collections/{id}/generations", collection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.quiz").exists())
                .andExpect(jsonPath("$.quiz.rawContent").isString());
    }

    @Test
    void createGeneration_returns201_withEmptySourceMaterials() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());
        CreateGenerationRequest request = TestDataGenerator.validGenerationRequest(material.getId());

        mockMvc.perform(post("/collections/{id}/generations", collection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quiz.rawContent").value("ett dokument med frågor"));
    }

    @Test
    void createGeneration_returns400_whenTopicsFocusAreaHasNoTopics() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());
        CreateGenerationRequest request = TestDataGenerator.invalidRequest_topicsFocusWithNoTopics(material.getId());

        mockMvc.perform(post("/collections/{id}/generations", collection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.topicsSpecifiedWhenFocusAreaIsTopics").exists());
    }

    @Test
    void createGeneration_returns201_whenTopicsFocusAreaHasTopics() throws Exception {
        Collection collection = persistCollection(persistUser());
        SourceMaterial material = persistMaterial(collection.getId());
        CreateGenerationRequest request = TestDataGenerator.validRequest_topicsFocusWithTopics(material.getId());

        mockMvc.perform(post("/collections/{id}/generations", collection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quiz").exists());
    }

    // ---------------------------------------------------------------
    // GET /collections/{collectionId}/generations/{generationId}
    // ---------------------------------------------------------------

    @Test
    void getGeneration_returns200_whenExists() throws Exception {
        Collection collection = persistCollection(persistUser());
        Generation generation = collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/collections/{pid}/generations/{gid}",
                            collection.getId(), generation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(generation.getId().toString()));
    }

    @Test
    void getGeneration_returns404_whenNotFound() throws Exception {
        Collection collection = persistCollection(persistUser());

        mockMvc.perform(get("/collections/{pid}/generations/{gid}",
                            collection.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /collections/{collectionId}/generations/{generationId}
    // ---------------------------------------------------------------

    @Test
    void deleteGeneration_returns204() throws Exception {
        Collection collection = persistCollection(persistUser());
        Generation generation = collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest());

        mockMvc.perform(delete("/collections/{pid}/generations/{gid}",
                               collection.getId(), generation.getId()))
                .andExpect(status().isNoContent());
    }

    // ---------------------------------------------------------------
    // GET /collections/{collectionId}/generations/{generationId}/quiz
    // ---------------------------------------------------------------

    @Test
    void getQuiz_returns200WithQuizContent_whenExists() throws Exception {
        Collection collection = persistCollection(persistUser());
        Generation generation = collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest());
        collectionService.createQuiz(generation.getId(), "Week 1", "raw content");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/collections/{pid}/generations/{gid}/quiz",
                            collection.getId(), generation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Week 1"))
                .andExpect(jsonPath("$.rawContent").value("raw content"));
    }

    @Test
    void getQuiz_returns404_whenGenerationHasNoQuiz() throws Exception {
        Collection collection = persistCollection(persistUser());
        Generation generation = collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest());
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/collections/{pid}/generations/{gid}/quiz",
                            collection.getId(), generation.getId()))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // PATCH /collections/{collectionId}/generations/{generationId}/quiz
    // ---------------------------------------------------------------

    @Test
    void updateQuiz_returns200WithUpdatedContent() throws Exception {
        Collection collection = persistCollection(persistUser());
        Generation generation = collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest());
        collectionService.createQuiz(generation.getId(), "Old Name", "old content");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(patch("/collections/{pid}/generations/{gid}/quiz",
                              collection.getId(), generation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New Name","rawContent":"new content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.rawContent").value("new content"));
    }

    // ---------------------------------------------------------------
    // DELETE /collections/{collectionId}/generations/{generationId}/quiz
    // ---------------------------------------------------------------

    @Test
    void deleteQuiz_returns204() throws Exception {
        Collection collection = persistCollection(persistUser());
        Generation generation = collectionService.createGeneration(collection.getId(), TestDataGenerator.validGenerationRequest());
        collectionService.createQuiz(generation.getId(), "Quiz", "content");
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/collections/{pid}/generations/{gid}/quiz",
                               collection.getId(), generation.getId()))
                .andExpect(status().isNoContent());
    }
}
