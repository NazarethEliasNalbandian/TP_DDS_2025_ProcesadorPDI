package ar.edu.utn.dds.k3003.controller.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PdIResponseDTO(
        String id,
        @JsonProperty("hecho_id") @JsonAlias({"hecho_id","hechoId"}) String hechoId,
        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,
        List<String> etiquetas,

        @JsonProperty("image_url")
        String imageUrl // 👈 NUEVO
) {}
