package se.ltu.eduo.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import se.ltu.eduo.repository.SourceMaterialRepository;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class FileService {

    private final SourceMaterialRepository sourceMaterialRepository;

    public String getFileAsString(UUID sourceMaterialId) {

        byte[] fileData = sourceMaterialRepository.findById(sourceMaterialId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Source material not found: " + sourceMaterialId))
                .getFileData();

        return new String(fileData, StandardCharsets.UTF_8);
    }
}



