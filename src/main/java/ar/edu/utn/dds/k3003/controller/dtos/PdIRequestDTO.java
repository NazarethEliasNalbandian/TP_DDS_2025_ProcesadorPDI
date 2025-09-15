package ar.edu.utn.dds.k3003.controller.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PdIRequestDTO(
        @JsonProperty("hecho_id") @JsonAlias({"hecho_id","hechoId"}) String hechoId,
        String descripcion,
        String lugar,
        LocalDateTime momento,
        String contenido,
        List<String> etiquetas,

        @JsonProperty("image_url")
        @JsonAlias({"image_url","imageUrl","url_imagen"})
        String imageUrl // 👈 NUEVO
) {}
