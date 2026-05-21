package se.ltu.eduo.llm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.collection.dto.GenerationDto;
import se.ltu.eduo.collection.request.CreateGenerationRequest;
import se.ltu.eduo.collection.mapper.GenerationMapper;
import se.ltu.eduo.collection.mapper.QuizMapper;
import se.ltu.eduo.collection.model.Generation;
import se.ltu.eduo.collection.model.Quiz;
import se.ltu.eduo.collection.service.CollectionService;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class StudyQuestionService {

    private final CollectionService collectionService;
    private final FileService fileService;
    private final PromptService promptService;
    private final LlmService llmService;
    private final GenerationMapper generationMapper;
    private final SettingsService settingsService;
    private final QuizMapper quizMapper;


    @Transactional
    public GenerationDto generateStudyQuestions(UUID collectionId, CreateGenerationRequest request) {

        // Vid generering skapas en tabell för generering i databasen
        // Detta kräver både collectiontId och en CreateGenerationRequest
        Generation generation = collectionService.createGeneration(collectionId, request);

        // Concatenate the extracted text from all selected source materials
        List<UUID> sourceMaterialIds = List.of(request.sourceMaterials());
        String fileContent = fileService.getFilesAsString(sourceMaterialIds);

        // Hämtar en färdig string med settingsinstruktioner
        String settings = settingsService.allSettings(request);

        // Stringen är bara en del av promten, vi behöver också lägga till instruktioner
        // Detta görs via promtService
        String prompt = promptService.buildPrompt(fileContent, settings);

        // llmService skickar promten till llm och tar emot en string output
        String output = llmService.generateStudyQuestions(prompt);

        //Vi behöver skapa en quiz tabell där vi lagrar output
        Quiz quiz = collectionService.createQuiz(
                generation.getId(),
                "Quiz " + generation.getId(),
                output
        );

        //lägg till skapad quiz i generation
        generation.setQuiz(quiz);

        //konvertera till dto och returnerar
        return generationMapper.toDto(generation);
    }
}







