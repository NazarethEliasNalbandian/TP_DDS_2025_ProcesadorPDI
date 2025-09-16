package ar.edu.utn.dds.k3003.controller.dtos;

import ar.edu.utn.dds.k3003.model.PdI;

import java.util.List;

public record ProcesamientoResponseDTO(
        String pdiId,
        PdI.ProcessingState estado,
        List<String> autoTags,
        String ocrText
) {}

