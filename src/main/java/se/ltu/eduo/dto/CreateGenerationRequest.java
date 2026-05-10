package se.ltu.eduo.dto;

import java.util.List;
import java.util.UUID;

public record CreateGenerationRequest(List<UUID> sourceMaterialIds) {}