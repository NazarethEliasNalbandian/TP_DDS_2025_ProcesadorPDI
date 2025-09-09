package ar.edu.utn.dds.k3003.controller.dtos;

import java.util.List;

public record ProcesamientoResponseDTO(String pdiId, boolean procesada, List<String> etiquetas) {}
