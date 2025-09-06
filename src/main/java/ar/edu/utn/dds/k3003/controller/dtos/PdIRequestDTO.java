package ar.edu.utn.dds.k3003.controller.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PdIRequestDTO(
        @JsonProperty("hecho_id")               // así se serializa siempre como hecho_id
        @JsonAlias({"hecho_id", "hechoId"})     // acepta tanto snake_case como camelCase al deserializar
        String hechoId,
        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,
        List<String> etiquetas
) {}
