package se.ltu.eduo.dto.request;

import java.util.List;
import java.util.UUID;

public record CreateGenerationRequest(List<UUID> sourceMaterialIds) {
    //lista av source materials
    //alla inställningar som ska användas
    //vilken collection det hör till (oh därmed användare)
    //
}