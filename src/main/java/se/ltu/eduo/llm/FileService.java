package se.ltu.eduo.llm;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.ltu.eduo.collection.repository.SourceMaterialRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class FileService {

    private final SourceMaterialRepository sourceMaterialRepository;

    public String getFileAsString(UUID sourceMaterialId) {
        String extractedText = sourceMaterialRepository.findById(sourceMaterialId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Source material not found: " + sourceMaterialId))
                .getExtractedText();
        return extractedText != null ? extractedText : "";
    }

    public String getFilesAsString(List<UUID> sourceMaterialIds) {
        return sourceMaterialIds.stream()
                .map(this::getFileAsString)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}



