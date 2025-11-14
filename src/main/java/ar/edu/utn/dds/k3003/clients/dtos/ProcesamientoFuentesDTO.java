package ar.edu.utn.dds.k3003.clients.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ProcesamientoFuentesDTO(
        String pdiId,
        @JsonProperty("estado") String estado,
        @JsonProperty("auto_tags") List<String> autoTags
) {}
