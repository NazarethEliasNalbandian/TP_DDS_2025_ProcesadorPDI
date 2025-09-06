package ar.edu.utn.dds.k3003.controller.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PdIResponseDTO(
        String id,

        @JsonProperty("hecho_id")               // en la response siempre sale "hecho_id"
        @JsonAlias({"hecho_id", "hechoId"})     // acepta ambas variantes al deserializar
        String hechoId,

        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,
        List<String> etiquetas
) {}
