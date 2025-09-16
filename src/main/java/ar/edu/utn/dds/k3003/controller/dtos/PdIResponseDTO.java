// src/main/java/ar/edu/utn/dds/k3003/controller/dtos/PdIResponseDTO.java
package ar.edu.utn.dds.k3003.controller.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record PdIResponseDTO(
        String id,
        @JsonProperty("hecho_id") String hechoId,
        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,

        @JsonProperty("auto_tags")
        List<String> autoTags,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("estado")
        String processingState,

        @JsonProperty("ocr_text")
        String ocrText,

        @JsonProperty("processed_at")
        LocalDateTime processedAt,

        @JsonProperty("last_error")
        String lastError
) {}
