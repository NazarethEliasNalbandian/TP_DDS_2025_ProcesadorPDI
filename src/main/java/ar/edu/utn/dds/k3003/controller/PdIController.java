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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public PdIController(
            FachadaProcesadorPDI fachadaProcesadorPdI,
            @Qualifier("solicitudesRetrofitProxy")
            ar.edu.utn.dds.k3003.facades.FachadaSolicitudes solicitudes,
            RabbitTemplate rabbitTemplate) {

        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
        this.solicitudes = solicitudes;
        this.rabbitTemplate = rabbitTemplate;
    }


    // GET /api/pdis?hecho={hechoId}  |  GET /api/pdis
    @GetMapping
    public ResponseEntity<List<PdIResponseDTO>> listarPdisPorHecho(
            @RequestParam(name = "hecho", required = false) String hechoId) {

        log.info("Hecho recibido → hechoId={}", hechoId);

        List<PdIDTO> lista = (hechoId != null)
                ? fachadaProcesadorPdI.buscarPorHecho(hechoId)
                : fachadaProcesadorPdI.pdis();

        log.info("Resultado consulta PdIs → hechoId={} count={}", hechoId, lista.size());

        return ResponseEntity.ok(lista.stream().map(this::toResponse).toList());
    }

    // GET /api/pdis/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PdIResponseDTO> obtenerPdiPorId(@PathVariable String id) {
        PdIDTO dto = fachadaProcesadorPdI.buscarPdIPorId(id);
        if (dto == null) throw new NoSuchElementException("No se encontró el PdI con id=" + id);
        return ResponseEntity.ok(toResponse(dto));
    }

    @PostMapping
    public ResponseEntity<ProcesamientoResponseDTO> procesarNuevoPdi(@RequestBody PdIRequestDTO req) {
        log.info("[ProcesadorPdI] Nuevo request recibido: hechoId={}, descripcion={}, imageUrl={}",
                req.hechoId(), req.descripcion(), req.imageUrl());

        boolean activo = this.solicitudes.estaActivo(req.hechoId());
        if (!activo) {
            log.warn("[ProcesadorPdI] Hecho {} inactivo, abortando procesamiento", req.hechoId());
            return ResponseEntity.internalServerError().build();
        }

        // 🔹 1️⃣ Crear entidad PdI en estado PENDING
        PdI nuevo = new PdI(
                req.hechoId(),
                req.descripcion(),
                req.lugar(),
                req.momento(),
                req.contenido(),
                req.imageUrl()
        );
        nuevo.setProcessingState(PdI.ProcessingState.PENDING);

        PdI guardado = fachadaProcesadorPdI.guardarPendiente(nuevo);

        log.info("[ProcesadorPdI] PdI guardado con id={} y estado=PENDING", guardado.getId());

        // 🔹 2️⃣ Enviar ID a la cola de trabajo
        rabbitTemplate.convertAndSend("pdi.direct", "pdi.process", guardado.getId());
        log.info("[ProcesadorPdI] ID={} enviado a cola 'pdi.process'", guardado.getId());

        // 🔹 3️⃣ Responder inmediatamente (asincrónico)
        return ResponseEntity.accepted().body(new ProcesamientoResponseDTO(
                String.valueOf(guardado.getId()),
                PdI.ProcessingState.PENDING,
                List.of()
        ));
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
