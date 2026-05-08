package se.ltu.eduo.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.model.project.*;

import java.util.List;
import java.util.UUID;


@Service
public class StudyQuestionService {

    private final ProjectService projectService;
    private final FileService fileService;
    private final PromptService promptService;
    private final LlmService llmService;

    public StudyQuestionService(
            ProjectService projectService,
            FileService fileService,
            PromptService promptService,
            LlmService llmService
    ) {
        this.projectService = projectService;
        this.fileService = fileService;
        this.promptService = promptService;
        this.llmService = llmService;
    }


    @Transactional
    public UUID generateStudyQuestions(UUID projectId, UUID sourceMaterialId) {

        // Vid generering skapas en tabell för generering i databasen
        // Detta kräver både projectId och sourceMaterialId
        Generation generation = projectService.createGeneration(projectId, List.of(sourceMaterialId));

        // Efter genererings tabellen har skapad behöver vi en string av filen
        // som ska in till llm.
        String fileContent = fileService.getFileAsString(sourceMaterialId);

        // Stringen är bara en del av promten, vi behöver också lägga till instruktioner
        // Detta görs via promtService
        String prompt = promptService.buildPrompt(fileContent);

        // llmService skickar promten till llm och tar emot en string output
        String output = llmService.generateStudyQuestions(prompt);

        //Vi behöver skapa en quiz tabell där vi lagrar output
        projectService.createQuiz(
                generation.getId(),
                "Quiz " + generation.getId(),
                output
        );

        //returnerar bara id sålänge, inte output
        return generation.getId();
    }
}







