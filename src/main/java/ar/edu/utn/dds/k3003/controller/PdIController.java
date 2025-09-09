package ar.edu.utn.dds.k3003.controller;

import ar.edu.utn.dds.k3003.controller.dtos.ProcesamientoResponseDTO;
import ar.edu.utn.dds.k3003.exceptions.domain.pdi.HechoInactivoException;
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

    @PostMapping
    public ResponseEntity<ProcesamientoResponseDTO> procesarNuevoPdi(@RequestBody PdIRequestDTO req) {
        System.out.println("ProcesadorPdI ← Fuentes (req DTO): " + req);

        PdIDTO entrada = new PdIDTO(
                null,
                req.hechoId(),
                req.descripcion(),
                req.lugar(),
                req.momento(),
                req.contenido(),
                req.etiquetas() == null ? List.of() : req.etiquetas()
        );
        System.out.println("ProcesadorPdI mapea a PdIDTO: " + entrada);

        try {
            PdIDTO procesado = fachadaProcesadorPdI.procesar(entrada);

            String pdiId = procesado.id() != null ? String.valueOf(procesado.id()) : null;
            var etiquetas = (procesado.etiquetas() != null) ? procesado.etiquetas() : List.of();

            // Procesada OK (nueva o duplicada)
            return ResponseEntity.ok(new ProcesamientoResponseDTO(pdiId, true, (List<String>) etiquetas));

        } catch (HechoInactivoException e) {
            // 200 con procesada=false, etiquetas vacías y pdiId=null
            return ResponseEntity.ok(new ProcesamientoResponseDTO(null, false, List.of()));
        }
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
