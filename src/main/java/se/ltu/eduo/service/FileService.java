package se.ltu.eduo.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Service
public class FileService {

    private final ProjectService projectService;

    public FileService(ProjectService projectService) {
        this.projectService = projectService;
    }

    public String getFileAsString(UUID sourceMaterialId) {
        byte[] fileData = projectService.getSourceMaterial(sourceMaterialId).getFileData();
        return new String(fileData, StandardCharsets.UTF_8);
    }
}


