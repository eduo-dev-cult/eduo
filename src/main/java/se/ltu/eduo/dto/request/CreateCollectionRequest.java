package se.ltu.eduo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateCollectionRequest(
        @NotBlank(message = "must be associated with a user")
        Integer userId,
        @NotBlank(message = "collection must have a name")
        String name) {}