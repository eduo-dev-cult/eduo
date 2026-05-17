package se.ltu.eduo.controller;

import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final MultipartProperties multipartProperties;

    public ConfigController(MultipartProperties multipartProperties) {
        this.multipartProperties = multipartProperties;
    }

    @GetMapping
    public ResponseEntity<Map<String, Long>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "maxFileSizeBytes", multipartProperties.getMaxFileSize().toBytes()
        ));
    }
}