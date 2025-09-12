package ar.edu.utn.dds.k3003.clients.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record HechoResponseDTO(
        @JsonAlias({"hechoId", "id"})
        @JsonProperty("id")           // opcional: si querés serializar como "id"
        String hechoId,

        // Usar wrapper para detectar null explícitamente
        Boolean activo
) {}
