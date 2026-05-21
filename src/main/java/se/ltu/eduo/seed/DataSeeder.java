package se.ltu.eduo.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.collection.model.Collection;
import se.ltu.eduo.collection.model.Generation;
import se.ltu.eduo.collection.model.GenerationFocusArea;
import se.ltu.eduo.collection.model.GenerationLanguage;
import se.ltu.eduo.collection.model.GenerationSourceMaterial;
import se.ltu.eduo.collection.model.Quiz;
import se.ltu.eduo.collection.model.SourceMaterial;
import se.ltu.eduo.collection.repository.CollectionRepository;
import se.ltu.eduo.collection.service.DocumentTextExtractor;
import se.ltu.eduo.collection.repository.GenerationRepository;
import se.ltu.eduo.collection.repository.GenerationSourceMaterialRepository;
import se.ltu.eduo.collection.repository.QuizRepository;
import se.ltu.eduo.collection.repository.SourceMaterialRepository;
import se.ltu.eduo.user.model.User;
import se.ltu.eduo.user.model.UserCredential;
import se.ltu.eduo.user.model.UserPreferences;
import se.ltu.eduo.user.repository.UserCredentialRepository;
import se.ltu.eduo.user.repository.UserPreferencesRepository;
import se.ltu.eduo.user.repository.UserRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the database with two prototype test users and their associated
 * projects on first boot (skipped when any users already exist).
 *
 * <p>Drop PDF files into {@code src/main/resources/seed-data/alice/<project>/}
 * and {@code src/main/resources/seed-data/bob/<project>/}. Each subdirectory
 * becomes one project; Alice's projects get a completed quiz, Bob's are left
 * in the pre-generation state for live demos.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46}; // %PDF

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final CollectionRepository collectionRepository;
    private final SourceMaterialRepository sourceMaterialRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final GenerationRepository generationRepository;
    private final GenerationSourceMaterialRepository generationSourceMaterialRepository;
    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (userRepository.count() > 0) {
            log.info("Users already present — skipping seed.");
            return;
        }

        log.info("Seeding database with prototype test data...");

        User alice = createUser("Alice", "Svensson", "alisve-5", "sv-SE");
        User bob = createUser("Bob", "Lindqvist", "boblin-3", "en-GB");

        seedProjects(alice, "alice", true);
        seedProjects(bob, "bob", false);

        log.info("Seeding complete.");
    }

    private User createUser(String firstName, String lastName, String username, String locale) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user = userRepository.save(user);

        UserCredential cred = new UserCredential();
        cred.setUser(user);
        cred.setUsername(username);
        cred.setPassword("placeholder");
        userCredentialRepository.save(cred);

        UserPreferences prefs = new UserPreferences();
        prefs.setUser(user);
        prefs.setLocale(locale);
        userPreferencesRepository.save(prefs);

        return user;
    }

    private void seedProjects(User owner, String userFolder, boolean withGenerations) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] pdfs = resolver.getResources("classpath:seed-data/" + userFolder + "/**/*.pdf");

        if (pdfs.length == 0) {
            log.warn("No PDFs found under seed-data/{} — no projects created for {}.", userFolder, owner.getFirstName());
            return;
        }

        Map<String, List<Resource>> byProject = groupByProjectFolder(pdfs, userFolder);

        for (Map.Entry<String, List<Resource>> entry : byProject.entrySet()) {
            seedProject(owner, entry.getKey(), entry.getValue(), withGenerations);
        }
    }

    private Map<String, List<Resource>> groupByProjectFolder(Resource[] pdfs, String userFolder) throws IOException {
        String marker = "seed-data/" + userFolder + "/";
        Map<String, List<Resource>> byProject = new LinkedHashMap<>();

        for (Resource pdf : pdfs) {
            String path = pdf.getURL().getPath();
            int idx = path.lastIndexOf(marker);
            if (idx == -1) continue;

            String relative = path.substring(idx + marker.length());
            int slash = relative.indexOf('/');
            if (slash == -1) continue; // file directly under userFolder, not inside a project folder

            String projectFolder = relative.substring(0, slash);
            byProject.computeIfAbsent(projectFolder, k -> new ArrayList<>()).add(pdf);
        }

        return byProject;
    }

    private void seedProject(User owner, String folderName, List<Resource> pdfResources, boolean withGeneration) throws IOException {
        String projectName = toProjectName(folderName);
        Collection project = collectionRepository.save(new Collection(owner, projectName));

        List<SourceMaterial> materials = new ArrayList<>();
        for (Resource pdf : pdfResources) {
            byte[] bytes = loadAndValidatePdf(pdf);
            if (bytes == null) continue;
            materials.add(sourceMaterialRepository.save(
                    new SourceMaterial(project, pdf.getFilename(), "application/pdf", bytes, documentTextExtractor.extractText(bytes, "application/pdf"))));
        }

        if (materials.isEmpty()) {
            log.warn("No valid PDFs in '{}' — project created with no materials.", folderName);
            return;
        }

        log.info("Seeded project '{}' for {} ({} file(s)){}.",
                projectName, owner.getFirstName(), materials.size(),
                withGeneration ? " + quiz" : "");

        if (!withGeneration) return;

        Generation gen = buildGeneration(project);
        gen = generationRepository.save(gen);

        for (SourceMaterial m : materials) {
            generationSourceMaterialRepository.save(new GenerationSourceMaterial(gen, m));
        }

        quizRepository.save(new Quiz(gen, projectName + " Quiz", SAMPLE_QUIZ_CONTENT));
    }

    private Generation buildGeneration(Collection project) {
        Generation gen = new Generation(project);
        gen.setNumOfQuestions(10);
        gen.setLanguage(GenerationLanguage.ENGLISH);
        gen.setFocusArea(GenerationFocusArea.KEY_CONCEPTS);
        gen.setEasy(true);
        gen.setMedium(true);
        gen.setHard(false);
        gen.setMultipleChoice(true);
        gen.setOpenEnded(false);
        gen.setTrueFalse(true);
        gen.setQuestions(true);
        gen.setCorrectAnswers(true);
        gen.setExplanations(true);
        gen.setDescription(true);
        return gen;
    }

    private byte[] loadAndValidatePdf(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            if (!hasPdfMagicBytes(bytes)) {
                log.warn("Skipping '{}': magic bytes do not match PDF.", resource.getFilename());
                return null;
            }
            return bytes;
        } catch (IOException e) {
            log.warn("Could not read '{}': {}", resource.getFilename(), e.getMessage());
            return null;
        }
    }

    private boolean hasPdfMagicBytes(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) return false;
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) return false;
        }
        return true;
    }

    private String toProjectName(String folderName) {
        return Arrays.stream(folderName.split("[_\\-]+"))
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(folderName);
    }

    private static final String SAMPLE_QUIZ_CONTENT = """
            {
              "questions": [
                {
                  "question": "Which of the following best describes the primary focus of the lecture material?",
                  "type": "multiple_choice",
                  "difficulty": "medium",
                  "options": [
                    "A) Real-time data stream processing",
                    "B) Systematic analysis using the core concepts from the material",
                    "C) Distributed consensus algorithms",
                    "D) Low-level memory management"
                  ],
                  "correct_answer": "B",
                  "explanation": "The material centres on structured, principled approaches to the domain's key problems."
                },
                {
                  "question": "What distinguishes the approach described in the material from ad-hoc methods?",
                  "type": "multiple_choice",
                  "difficulty": "medium",
                  "options": [
                    "A) It relies solely on empirical trial and error",
                    "B) It provides no formal guarantees",
                    "C) It applies a repeatable, theory-grounded methodology",
                    "D) It is limited to small-scale problems"
                  ],
                  "correct_answer": "C",
                  "explanation": "A repeatable, theory-grounded methodology is what separates rigorous approaches from ad-hoc ones."
                },
                {
                  "question": "True or False: The techniques presented in the material are restricted to academic use and cannot be applied in industry.",
                  "type": "true_false",
                  "difficulty": "easy",
                  "correct_answer": "False",
                  "explanation": "The techniques are widely adopted in both research and industrial practice."
                },
                {
                  "question": "True or False: Understanding the foundational theory is a prerequisite for correctly applying the methods discussed.",
                  "type": "true_false",
                  "difficulty": "easy",
                  "correct_answer": "True",
                  "explanation": "Without the theoretical grounding, practitioners risk misapplying the methods and producing incorrect results."
                },
                {
                  "question": "Explain in your own words why the methodology described is preferable to simpler alternatives for non-trivial problems.",
                  "type": "open_ended",
                  "difficulty": "hard",
                  "sample_answer": "For non-trivial problems, simpler alternatives often break down because they lack the formal structure needed to handle edge cases and scale. The methodology described provides a rigorous framework that remains tractable and correct across a wider range of inputs."
                }
              ]
            }
            """;
}
