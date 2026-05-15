package se.ltu.eduo.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.dto.GenerationDto;
import se.ltu.eduo.dto.request.CreateGenerationRequest;
import se.ltu.eduo.mapper.GenerationMapper;
import se.ltu.eduo.mapper.QuizMapper;
import se.ltu.eduo.model.collection.Generation;
import se.ltu.eduo.model.collection.Quiz;
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

        // extraherar första id från en lista av sourceMaterialId
        UUID sourceMaterialId = request.sourceMaterials()[0]; //TODO use all materials rather than only the first

        // Efter genererings tabellen har skapad behöver vi en string av filen
        // som ska in till llm.
        String fileContent = fileService.getFileAsString(sourceMaterialId);

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







