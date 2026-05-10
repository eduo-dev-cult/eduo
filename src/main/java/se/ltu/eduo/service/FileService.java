package se.ltu.eduo.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Service
public class FileService {

    private final CollectionService collectionService;

    public FileService(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    public String getFileAsString(UUID sourceMaterialId) {
        byte[] fileData = collectionService.getSourceMaterial(sourceMaterialId).getFileData();
        return new String(fileData, StandardCharsets.UTF_8);
    }
}


