package se.ltu.eduo.utils;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        long maxBytes = ex.getMaxUploadSize();
        Long actualBytes = extractActualSize(ex);

        String message;
        if (actualBytes != null && maxBytes > 0) {
            message = String.format("File of %s exceeds the maximum upload size of %s.",
                    formatMb(actualBytes), formatMb(maxBytes));
        } else if (maxBytes > 0) {
            message = String.format("File exceeds the maximum upload size of %s.", formatMb(maxBytes));
        } else {
            message = "File exceeds the maximum upload size.";
        }

        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of(
                "status", HttpStatus.CONTENT_TOO_LARGE.value(),
                "error", message,
                "timestamp", Instant.now().toString()
        ));
    }

    // Walks the cause chain for an underlying exception exposing getActualSize()
    // (e.g. Tomcat/Commons FileUpload's FileSizeLimitExceededException). Reflection
    // avoids a hard dependency on the servlet container's internal package.
    private static Long extractActualSize(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            try {
                Object result = c.getClass().getMethod("getActualSize").invoke(c);
                if (result instanceof Long bytes) {
                    return bytes;
                }
            } catch (ReflectiveOperationException ignored) {
                // method not present on this exception type — keep walking
            }
        }
        return null;
    }

    private static String formatMb(long bytes) {
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                                            .getFieldErrors()
                                            .stream()
                                            .collect(Collectors.toMap(
                                                    FieldError::getField,
                                                    fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                                                    (existing, duplicate) -> existing   // keep first if same field has multiple violations
                                            ));

        Map<String, Object> body = Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Validation failed",
                "fields", fieldErrors,
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.badRequest().body(body);
    }
}
