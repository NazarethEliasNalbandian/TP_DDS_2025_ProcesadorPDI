package ar.edu.utn.dds.k3003.controller;

import ar.edu.utn.dds.k3003.controller.dtos.ProcesamientoResponseDTO;
import ar.edu.utn.dds.k3003.controller.dtos.PdIRequestDTO;
import ar.edu.utn.dds.k3003.controller.dtos.PdIResponseDTO;
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInactivoException;
import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.model.PdI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PdIController.class);

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

        // log.info("Resultado consulta PdIs → hechoId={} count={}", hechoId, lista.size());

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
        log.info("[ProcesadorPdI] Nuevo request recibido: hechoId={}, descripcion={}, imageUrl={}",
                req.hechoId(), req.descripcion(), req.imageUrl());

        boolean activo = this.solicitudes.estaActivo(req.hechoId());
        log.info("[ProcesadorPdI] Estado del hechoId={} → activo={}", req.hechoId(), activo);

        if (!activo) {
            log.info("[ProcesadorPdI] Hecho {} inactivo, abortando procesamiento", req.hechoId());
            return ResponseEntity.ok(new ProcesamientoResponseDTO(
                    null,
                    PdI.ProcessingState.ERROR,
                    List.of()
            ));
        }

        PdIDTO entrada = new PdIDTO(
                null,
                req.hechoId(),
                req.descripcion(),
                req.lugar(),
                req.momento(),
                req.contenido(),
                req.imageUrl(),
                List.of(),
                null,
                (req.imageUrl() != null && !req.imageUrl().isBlank())
                        ? PdI.ProcessingState.PENDING
                        : PdI.ProcessingState.PROCESSED,
                null,
                null
        );
        log.info("[ProcesadorPdI] DTO inicial armado: {}", entrada);

        try {
            PdIDTO procesado = fachadaProcesadorPdI.procesar(entrada);
            log.info("[ProcesadorPdI] Procesamiento exitoso para hechoId={} → id={}, state={}, tags={}",
                    procesado.hechoId(), procesado.id(), procesado.processingState(), procesado.autoTags());

            return ResponseEntity.ok(new ProcesamientoResponseDTO(
                    procesado.id(),
                    procesado.processingState(),
                    (procesado.autoTags() != null) ? procesado.autoTags() : List.of()
            ));

        } catch (HechoInactivoException e) {
            log.info("[ProcesadorPdI] Hecho {} marcado como inactivo en fachada", req.hechoId(), e);
            return ResponseEntity.ok(new ProcesamientoResponseDTO(
                    null,
                    PdI.ProcessingState.ERROR,
                    List.of()
            ));
        } catch (Exception e) {
            log.info("[ProcesadorPdI] Error inesperado procesando hechoId={}: {}", req.hechoId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
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
                p.autoTags(),
                p.imageUrl(),
                (p.processingState() != null) ? p.processingState().name() : null,
                p.ocrText(),
                p.processedAt(),
                p.lastError()
        );
    }
}
