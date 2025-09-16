package ar.edu.utn.dds.k3003.controller;

import ar.edu.utn.dds.k3003.controller.dtos.ProcesamientoResponseDTO;
import ar.edu.utn.dds.k3003.controller.dtos.PdIRequestDTO;
import ar.edu.utn.dds.k3003.controller.dtos.PdIResponseDTO;
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInactivoException;
import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.model.PdI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/pdis")
public class PdIController {

    private final FachadaProcesadorPDI fachadaProcesadorPdI;
    private final ar.edu.utn.dds.k3003.facades.FachadaSolicitudes solicitudes;

    @Autowired
    public PdIController(
            FachadaProcesadorPDI fachadaProcesadorPdI,
            @Qualifier("solicitudesRetrofitProxy")
            ar.edu.utn.dds.k3003.facades.FachadaSolicitudes solicitudes) {
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
        this.solicitudes = solicitudes;
    }

    // GET /api/pdis?hecho={hechoId}  |  GET /api/pdis
    @GetMapping
    public ResponseEntity<List<PdIResponseDTO>> listarPdisPorHecho(
            @RequestParam(name = "hecho", required = false) String hechoId) {

        List<PdIDTO> lista = (hechoId != null)
                ? fachadaProcesadorPdI.buscarPorHecho(hechoId)
                : fachadaProcesadorPdI.pdis();

        return ResponseEntity.ok(lista.stream().map(this::toResponse).toList());
    }

    // GET /api/pdis/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PdIResponseDTO> obtenerPdiPorId(@PathVariable String id) {
        PdIDTO dto = fachadaProcesadorPdI.buscarPdIPorId(id);
        if (dto == null) throw new NoSuchElementException("No se encontró el PdI con id=" + id);
        return ResponseEntity.ok(toResponse(dto));
    }

    // POST /api/pdis
    @PostMapping
    public ResponseEntity<ProcesamientoResponseDTO> procesarNuevoPdi(@RequestBody PdIRequestDTO req) {
        // 1) Consenso estricto: si el hecho está inactivo, no procesamos
        boolean activo = this.solicitudes.estaActivo(req.hechoId());
        if (!activo) {
            return ResponseEntity.ok(new ProcesamientoResponseDTO(
                    null,
                    PdI.ProcessingState.ERROR,
                    List.of(),
                    null
            ));
        }

        // 2) Mapear request → PdIDTO (con campos nuevos inicializados)
        PdIDTO entrada = new PdIDTO(
                null,
                req.hechoId(),
                req.descripcion(),
                req.lugar(),
                req.momento(),
                req.contenido(),
                req.etiquetas(),              // (deprecated) se pasa por compatibilidad
                req.imageUrl(),
                List.of(),                    // autoTags inicial vacío
                null,                         // ocrText inicial
                (req.imageUrl() != null && !req.imageUrl().isBlank())
                        ? PdI.ProcessingState.PENDING   // si hay imagen, arranca PENDING (pipeline async)
                        : PdI.ProcessingState.PROCESSED,// sin imagen, etiquetado inmediato por contenido
                null,                         // processedAt
                null                          // lastError
        );

        try {
            // 3) Procesar en fachada (esta debe poblar los campos nuevos según su lógica)
            PdIDTO procesado = fachadaProcesadorPdI.procesar(entrada);

            // 4) Armar respuesta con lo que devolvió la fachada (sin inventar nada)
            return ResponseEntity.ok(new ProcesamientoResponseDTO(
                    procesado.id(),
                    procesado.processingState(),
                    (procesado.autoTags() != null) ? procesado.autoTags() : List.of(),
                    procesado.ocrText()
            ));

        } catch (HechoInactivoException e) {
            return ResponseEntity.ok(new ProcesamientoResponseDTO(
                    null,
                    PdI.ProcessingState.ERROR,
                    List.of(),
                    null
            ));
        }
    }

    // DELETE /api/pdis/purge
    @DeleteMapping("/purge")
    public ResponseEntity<Void> borrarTodo() {
        fachadaProcesadorPdI.borrarTodo();
        return ResponseEntity.noContent().build();
    }

    // ---------- mapper helper ----------
    private PdIResponseDTO toResponse(PdIDTO p) {
        // Ahora leemos todo directamente del PdIDTO extendido
        return new PdIResponseDTO(
                p.id(),
                p.hechoId(),
                p.descripcion(),
                p.lugar(),
                p.momento(),
                p.contenido(),
                (p.autoTags() != null && !p.autoTags().isEmpty()) ? p.autoTags() : p.etiquetas(), // fallback
                p.imageUrl(),
                (p.processingState() != null) ? p.processingState().name() : null,
                p.ocrText(),
                p.processedAt(),
                p.lastError()
        );
    }
}
