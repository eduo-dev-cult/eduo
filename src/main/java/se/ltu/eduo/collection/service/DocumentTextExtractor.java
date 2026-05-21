package se.ltu.eduo.collection.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentTextExtractor {

    private final Tika tika = new Tika();

    public String extractText(byte[] fileData, String fileType) {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("Cannot extract text from empty file data");
        }

        if ("text/plain".equals(fileType) || "text/markdown".equals(fileType)) {
            return normalizeText(new String(fileData, StandardCharsets.UTF_8));
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData)) {
            String extractedText = tika.parseToString(inputStream);
            return normalizeText(extractedText);
        } catch (IOException | TikaException e) {
            throw new RuntimeException("Failed to extract text from uploaded document", e);
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}