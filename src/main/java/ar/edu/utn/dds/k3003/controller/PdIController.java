package ar.edu.utn.dds.k3003.controller;

import ar.edu.utn.dds.k3003.facades.FachadaProcesadorPDI;
import ar.edu.utn.dds.k3003.facades.dtos.PdIDTO;
import ar.edu.utn.dds.k3003.controller.dtos.PdIRequestDTO;
import ar.edu.utn.dds.k3003.controller.dtos.PdIResponseDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pdis")
public class PdIController {

    private final FachadaProcesadorPDI fachadaProcesadorPdI;

    @Autowired
    public PdIController(FachadaProcesadorPDI fachadaProcesadorPdI) {
        this.fachadaProcesadorPdI = fachadaProcesadorPdI;
    }

    // GET /api/pdis?hecho={hechoId}
    // GET /api/pdis
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
        return ResponseEntity.ok(toResponse(dto));
    }

    // POST /api/pdis
    @PostMapping
    public ResponseEntity<PdIResponseDTO> procesarNuevoPdi(@RequestBody PdIRequestDTO req) {
        PdIDTO entrada = new PdIDTO(
                null,                         // id lo asigna el módulo dueño
                req.hechoId(),
                req.descripcion(),
                req.lugar(),
                req.momento(),
                req.contenido(),
                req.etiquetas() == null ? List.of() : req.etiquetas()
        );

        PdIDTO procesado = fachadaProcesadorPdI.procesar(entrada);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(procesado));
    }

    // DELETE /api/pdis/purge
    @DeleteMapping("/purge")
    public ResponseEntity<Void> borrarTodo() {
        fachadaProcesadorPdI.borrarTodo();
        return ResponseEntity.noContent().build();
    }

    // ---------- mappers ----------
    private PdIResponseDTO toResponse(PdIDTO p) {
        return new PdIResponseDTO(
                p.id(),
                p.hechoId(),
                p.descripcion(),
                p.lugar(),
                p.momento(),
                p.contenido(),
                p.etiquetas()
        );
    }
}
