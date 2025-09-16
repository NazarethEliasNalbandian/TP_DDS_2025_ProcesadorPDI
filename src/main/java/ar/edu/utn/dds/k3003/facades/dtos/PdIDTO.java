package ar.edu.utn.dds.k3003.facades.dtos;

import ar.edu.utn.dds.k3003.model.PdI;
import java.time.LocalDateTime;
import java.util.List;

public record PdIDTO(
        String id,
        String hechoId,
        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,

        @Deprecated
        List<String> etiquetas,   // ⚠️ Deprecated en Entrega 4

        String imageUrl,          // URL única de imagen

        List<String> autoTags,    // etiquetas generadas automáticamente
        String ocrText,           // texto extraído por OCR
        PdI.ProcessingState processingState, // estado del procesamiento
        LocalDateTime processedAt,
        String lastError
) {
    public PdIDTO(String id, String hechoId) {
        this(id, hechoId, null, null, null, null,
                List.of(), null, List.of(), null,
                PdI.ProcessingState.PENDING, null, null);
    }
}
